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

    // Jaccard por palavras: 0.6 = pelo menos 60% dos termos em comum.
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

        // 2) Fallback por similaridade de nome/descricao.
        String alvo = DescriptionNormalizer.normalize(movement.getDescricao());
        if (alvo.isBlank()) {
            return Optional.empty();
        }
        return learningRepository.findByScope(movement.getEmpresaId(), movement.getClienteId()).stream()
                .filter(h -> !h.getDescricaoPadrao().startsWith("#")) // ignora chaves de CNPJ
                .filter(h -> StringSimilarity.tokenSimilarity(alvo, h.getDescricaoPadrao())
                        >= HISTORY_THRESHOLD)
                .max((a, b) -> Integer.compare(a.getOcorrencias(), b.getOcorrencias()))
                .map(this::toSuggestion);
    }

    private SuggestedAccount toSuggestion(LearningHistory learned) {
        BigDecimal confianca = BigDecimal.valueOf(Math.min(90, 60 + learned.getOcorrencias() * 5));
        return new SuggestedAccount(learned.getContaId(), confianca);
    }
}
