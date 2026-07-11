package com.nalitech.modules.account.ai;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.movement.entity.Movement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Provedor de IA: usa um LLM (endpoint compativel com a API da OpenAI) para
 * sugerir a conta contabil. Habilitado apenas quando ha uma API key configurada;
 * caso contrario devolve vazio (o seletor cai na heuristica). Ver DEPLOY.md.
 */
@Slf4j
@Component
public class LlmSuggestionProvider implements AiSuggestionProvider {

    private static final int MAX_CONTAS_NO_PROMPT = 200;
    private static final BigDecimal CONFIANCA_IA = BigDecimal.valueOf(85);

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public LlmSuggestionProvider(@Value("${AI_API_URL:https://api.openai.com/v1}") String baseUrl,
                                 @Value("${AI_API_KEY:}") String apiKey,
                                 @Value("${AI_MODEL:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String origem() {
        return "IA";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<SuggestedAccount> suggest(Movement movement, List<ChartOfAccount> contas) {
        if (!isConfigured() || movement.getDescricao() == null || contas.isEmpty()) {
            return Optional.empty();
        }
        try {
            String prompt = montarPrompt(movement, contas);
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .body(Map.of(
                            "model", model,
                            "temperature", 0,
                            "messages", List.of(
                                    Map.of("role", "system", "content",
                                            "Voce e um assistente contabil. Responda APENAS com o codigo "
                                                    + "da conta contabil mais adequada, sem texto extra."),
                                    Map.of("role", "user", "content", prompt))))
                    .retrieve()
                    .body(Map.class);

            String codigo = extrairCodigo(response);
            if (codigo == null) {
                return Optional.empty();
            }
            return contas.stream()
                    .filter(c -> codigo.equalsIgnoreCase(c.getCodigo())
                            || codigo.contains(c.getCodigo()))
                    .findFirst()
                    .map(c -> new SuggestedAccount(c.getId(), CONFIANCA_IA));
        } catch (Exception ex) {
            log.warn("Falha ao consultar LLM para sugestao de conta: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String montarPrompt(Movement movement, List<ChartOfAccount> contas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lancamento: \"").append(movement.getDescricao()).append("\"");
        if (movement.getTipo() != null) {
            sb.append(" (").append(movement.getTipo().name()).append(")");
        }
        sb.append("\n\nPlano de contas disponivel (codigo - nome):\n");
        contas.stream().limit(MAX_CONTAS_NO_PROMPT).forEach(c ->
                sb.append(c.getCodigo()).append(" - ").append(c.getNome()).append("\n"));
        sb.append("\nQual o codigo da conta mais adequada? Responda so o codigo.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extrairCodigo(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            return null;
        }
        Object content = message.get("content");
        return content == null ? null : content.toString().trim();
    }
}
