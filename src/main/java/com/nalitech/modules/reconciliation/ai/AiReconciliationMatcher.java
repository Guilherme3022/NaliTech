package com.nalitech.modules.reconciliation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nalitech.modules.aiusage.service.AiUsageService;
import com.nalitech.modules.movement.entity.Movement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Validador de conciliacao por IA (camada "paliativa"): quando nenhuma camada
 * deterministica resolveu, envia a movimentacao + uma lista curta de candidatos
 * plausiveis a um LLM (endpoint compativel com a API da OpenAI) e pergunta qual
 * candidato corresponde a mesma transacao, com nivel de confianca e justificativa.
 *
 * <p>Pensado para rodar com IA <b>sem custo</b>: aponte {@code AI_API_URL} para
 * <b>Ollama</b> (local, {@code http://localhost:11434/v1}) ou <b>Groq</b>
 * (free tier). A IA so sugere; a confirmacao continua sendo humana.
 *
 * <p>Desligado por padrao. Ligue com {@code RECONCILIATION_AI_ENABLED=true}.
 */
@Slf4j
@Component
public class AiReconciliationMatcher {

    private static final int MAX_TOKENS_MOTIVO = 250;

    private final boolean enabled;
    private final String model;
    private final String reasoningEffort;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;

    public AiReconciliationMatcher(
            @Value("${RECONCILIATION_AI_ENABLED:false}") boolean enabled,
            @Value("${AI_API_URL:https://api.openai.com/v1}") String baseUrl,
            @Value("${AI_API_KEY:}") String apiKey,
            @Value("${AI_MODEL:gpt-4o-mini}") String model,
            @Value("${AI_REASONING_EFFORT:low}") String reasoningEffort,
            ObjectMapper objectMapper,
            AiUsageService aiUsageService) {
        this.enabled = enabled;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.objectMapper = objectMapper;
        this.aiUsageService = aiUsageService;
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        // Ollama (local) nao exige chave; so envia Authorization quando ha uma.
        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        this.restClient = builder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Pergunta ao LLM qual candidato corresponde a movimentacao alvo.
     * Devolve vazio se a IA estiver desligada, nao houver candidatos, ou a IA
     * nao identificar correspondencia (candidato 0).
     */
    @SuppressWarnings("unchecked")
    public Optional<AiMatch> findMatch(Movement target, List<Movement> candidates) {
        if (!enabled || target == null || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        String prompt = montarPrompt(target, candidates);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        // gpt-oss e modelo "reasoning": esforco baixo = menos tokens/custo e JSON mais limpo.
        // AI_REASONING_EFFORT= (vazio) omite o parametro, p/ provedores que nao o aceitam.
        if (StringUtils.hasText(reasoningEffort)) {
            body.put("reasoning_effort", reasoningEffort);
        }
        body.put("messages", List.of(
                java.util.Map.of("role", "system", "content",
                        "Voce e um assistente de conciliacao bancaria. Dada uma "
                                + "movimentacao e uma lista numerada de candidatos, "
                                + "identifique qual candidato representa a MESMA "
                                + "transacao (contrapartida). Responda APENAS com um "
                                + "objeto JSON, sem texto extra."),
                java.util.Map.of("role", "user", "content", prompt)));

        // Chamada ao LLM. Falhas de transporte (429/rede) PROPAGAM de proposito: o
        // chamador (sweep) precisa distinguir "indisponivel" (retentar depois) de
        // "avaliou e nao achou" (marcar como tentado). Nao engula o 429 aqui.
        java.util.Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(java.util.Map.class);

        registrarConsumo(target, response);

        // A resposta chegou: falha de parsing vira "sem match" (nao e indisponibilidade).
        try {
            String content = extrairConteudo(response);
            if (content == null) {
                return Optional.empty();
            }
            return interpretar(content, candidates);
        } catch (Exception ex) {
            log.warn("Falha ao interpretar resposta do LLM: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String montarPrompt(Movement target, List<Movement> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Movimentacao a conciliar:\n");
        sb.append("- descricao: \"").append(nvl(target.getDescricao())).append("\"\n");
        sb.append("- valor: ").append(target.getValor()).append("\n");
        sb.append("- data: ").append(target.getData()).append("\n");
        if (target.getTipo() != null) {
            sb.append("- tipo: ").append(target.getTipo().name()).append("\n");
        }
        sb.append("\nCandidatos:\n");
        for (int i = 0; i < candidates.size(); i++) {
            Movement c = candidates.get(i);
            sb.append(i + 1).append(". descricao: \"").append(nvl(c.getDescricao()))
                    .append("\" | valor: ").append(c.getValor())
                    .append(" | data: ").append(c.getData());
            if (c.getTipo() != null) {
                sb.append(" | tipo: ").append(c.getTipo().name());
            }
            sb.append("\n");
        }
        sb.append("\nResponda em JSON: {\"candidato\": <numero do candidato de 1 a ")
                .append(candidates.size())
                .append(", ou 0 se nenhum corresponder>, \"confianca\": <inteiro 0-100>, ")
                .append("\"motivo\": \"<justificativa breve>\"}");
        return sb.toString();
    }

    private Optional<AiMatch> interpretar(String content, List<Movement> candidates) throws Exception {
        String json = limparCercaDeCodigo(content);
        JsonNode node = objectMapper.readTree(json);
        int candidato = node.path("candidato").asInt(0);
        if (candidato < 1 || candidato > candidates.size()) {
            return Optional.empty();
        }
        int confianca = node.path("confianca").asInt(0);
        String motivo = node.path("motivo").asText("Correspondencia sugerida por IA");
        if (motivo.length() > MAX_TOKENS_MOTIVO) {
            motivo = motivo.substring(0, MAX_TOKENS_MOTIVO);
        }
        UUID matchedId = candidates.get(candidato - 1).getId();
        return Optional.of(new AiMatch(matchedId,
                BigDecimal.valueOf(Math.max(0, Math.min(100, confianca))), motivo));
    }

    @SuppressWarnings("unchecked")
    private void registrarConsumo(Movement target, java.util.Map<String, Object> response) {
        long entrada = 0;
        long saida = 0;
        Object usage = response == null ? null : response.get("usage");
        if (usage instanceof java.util.Map<?, ?> u) {
            entrada = asLong(u.get("prompt_tokens"));
            saida = asLong(u.get("completion_tokens"));
        }
        aiUsageService.record(target.getEmpresaId(), AiUsageService.FEATURE_CONCILIACAO, entrada, saida);
    }

    private long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private String extrairConteudo(java.util.Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        List<java.util.Map<String, Object>> choices =
                (List<java.util.Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        java.util.Map<String, Object> message =
                (java.util.Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            return null;
        }
        Object content = message.get("content");
        return content == null ? null : content.toString().trim();
    }

    // Remove cercas ```json ... ``` que alguns modelos adicionam em volta do JSON.
    private String limparCercaDeCodigo(String content) {
        String t = content.trim();
        if (t.startsWith("```")) {
            int primeiraQuebra = t.indexOf('\n');
            if (primeiraQuebra > 0) {
                t = t.substring(primeiraQuebra + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    /** Correspondencia sugerida pela IA: candidato + confianca (0-100) + justificativa. */
    public record AiMatch(UUID matchedMovementId, BigDecimal confianca, String justificativa) {
    }
}
