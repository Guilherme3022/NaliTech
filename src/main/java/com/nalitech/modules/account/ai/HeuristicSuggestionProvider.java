package com.nalitech.modules.account.ai;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import com.nalitech.modules.movement.entity.Movement;
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

    private static final double HISTORY_THRESHOLD = 0.8;

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
        if (movement.getDescricao() == null) {
            return Optional.empty();
        }
        return learningRepository.findByScope(movement.getEmpresaId(), movement.getClienteId()).stream()
                .filter(h -> StringSimilarity.ratio(movement.getDescricao(), h.getDescricaoPadrao())
                        >= HISTORY_THRESHOLD)
                .max((a, b) -> Integer.compare(a.getOcorrencias(), b.getOcorrencias()))
                .map(this::toSuggestion);
    }

    private SuggestedAccount toSuggestion(LearningHistory learned) {
        BigDecimal confianca = BigDecimal.valueOf(Math.min(90, 60 + learned.getOcorrencias() * 5));
        return new SuggestedAccount(learned.getContaId(), confianca);
    }
}
