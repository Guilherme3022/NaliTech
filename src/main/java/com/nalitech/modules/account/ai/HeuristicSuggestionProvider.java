package com.nalitech.modules.account.ai;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import com.nalitech.modules.account.service.LearningService;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.shared.util.DescriptionNormalizer;
import com.nalitech.shared.util.StringSimilarity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Provedor heuristico: sugere a conta a partir do historico de aprendizado do
 * cliente (descricao semelhante ja classificada antes). Zero custo, deterministico.
 */
@Component
public class HeuristicSuggestionProvider implements AiSuggestionProvider {

    // Coeficiente de sobreposicao minimo (|comuns| / min(tokens)) para aceitar o padrao aprendido.
    private static final double HISTORY_THRESHOLD = 0.6;

    private final LearningHistoryRepository learningRepository;

    public HeuristicSuggestionProvider(LearningHistoryRepository learningRepository) {
        this.learningRepository = learningRepository;
    }

    @Override
    public String origem() {
        return "HISTORICO";
    }

    @Override
    public Optional<SuggestedAccount> suggest(Movement movement, List<ChartOfAccount> contas) {
        // 1) Match exato pela contraparte (CNPJ/CPF): mais confiavel que o nome.
        String docKey = LearningService.documentoKey(movement.getDocumento());
        if (docKey != null) {
            Optional<SuggestedAccount> porDocumento = learningRepository
                    .findScoped(movement.getEmpresaId(), movement.getClienteId(), docKey)
                    .map(h -> new SuggestedAccount(h.getContaId(),
                            BigDecimal.valueOf(Math.min(95, 75 + h.getOcorrencias() * 5))));
            if (porDocumento.isPresent()) {
                return porDocumento;
            }
        }

        // 2) Fallback por similaridade de nome/descricao. Usa o COEFICIENTE DE SOBREPOSICAO
        // (|comuns| / min(tokens)) em vez de Jaccard, para o padrao aprendido (ex.: "black
        // decker") continuar casando mesmo quando a nova descricao vem com ruido (ex.:
        // "liquidacao boleto <cnpj> black decker"). Empata por nota e, depois, por ocorrencias.
        String alvo = DescriptionNormalizer.normalize(movement.getDescricao());
        if (alvo.isBlank()) {
            return Optional.empty();
        }
        int alvoTokens = StringSimilarity.tokenCount(alvo);

        LearningHistory melhor = null;
        double melhorNota = 0.0;
        for (LearningHistory h : learningRepository.findByScope(
                movement.getEmpresaId(), movement.getClienteId())) {
            String padrao = h.getDescricaoPadrao();
            if (padrao == null || padrao.startsWith("#")) {
                continue; // ignora chaves de CNPJ (tratadas no passo 1)
            }
            int comuns = StringSimilarity.commonTokenCount(alvo, padrao);
            if (comuns == 0) {
                continue;
            }
            // Guarda anti-falso-positivo: 2+ tokens distintivos em comum, ou um lado de token unico.
            int menorLado = Math.min(alvoTokens, StringSimilarity.tokenCount(padrao));
            if (comuns < 2 && menorLado != 1) {
                continue;
            }
            double nota = StringSimilarity.tokenOverlap(alvo, padrao);
            if (nota < HISTORY_THRESHOLD) {
                continue;
            }
            boolean melhorAtual = nota > melhorNota
                    || (nota == melhorNota && melhor != null
                        && h.getOcorrencias() > melhor.getOcorrencias());
            if (melhor == null || melhorAtual) {
                melhorNota = nota;
                melhor = h;
            }
        }
        return Optional.ofNullable(melhor).map(this::toSuggestion);
    }

    private SuggestedAccount toSuggestion(LearningHistory learned) {
        BigDecimal confianca = BigDecimal.valueOf(Math.min(90, 60 + learned.getOcorrencias() * 5));
        return new SuggestedAccount(learned.getContaId(), confianca);
    }
}
