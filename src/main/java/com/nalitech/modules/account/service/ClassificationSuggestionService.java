package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.entity.AiSuggestion;
import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.AiSuggestionRepository;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import com.nalitech.shared.util.StringSimilarity;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClassificationSuggestionService {

    private static final double HISTORY_THRESHOLD = 0.8;

    private final RuleEngineService ruleEngineService;
    private final LearningHistoryRepository learningRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final MovementRepository movementRepository;

    public ClassificationSuggestionService(RuleEngineService ruleEngineService,
                                           LearningHistoryRepository learningRepository,
                                           AiSuggestionRepository suggestionRepository,
                                           MovementRepository movementRepository) {
        this.ruleEngineService = ruleEngineService;
        this.learningRepository = learningRepository;
        this.suggestionRepository = suggestionRepository;
        this.movementRepository = movementRepository;
    }

    public AiSuggestion suggestFor(UUID movementId) {
        Movement movement = movementRepository
                .findByIdAndEmpresaId(movementId, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));

        Optional<AccountRule> rule = ruleEngineService.firstMatching(movement);
        if (rule.isPresent() && rule.get().getContaId() != null) {
            return persist(movement, rule.get().getContaId(), BigDecimal.valueOf(95), "REGRA");
        }

        Optional<LearningHistory> learned = bestFromHistory(movement);
        if (learned.isPresent()) {
            BigDecimal confianca = BigDecimal.valueOf(Math.min(90, 60 + learned.get().getOcorrencias() * 5));
            return persist(movement, learned.get().getContaId(), confianca, "HISTORICO");
        }

        return persist(movement, null, BigDecimal.ZERO, "NENHUMA");
    }

    private Optional<LearningHistory> bestFromHistory(Movement movement) {
        if (movement.getDescricao() == null) {
            return Optional.empty();
        }
        return learningRepository.findByEmpresaId(movement.getEmpresaId()).stream()
                .filter(h -> StringSimilarity.ratio(movement.getDescricao(), h.getDescricaoPadrao())
                        >= HISTORY_THRESHOLD)
                .max((a, b) -> Integer.compare(a.getOcorrencias(), b.getOcorrencias()));
    }

    private AiSuggestion persist(Movement movement, UUID contaId, BigDecimal confianca, String origem) {
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setEmpresaId(movement.getEmpresaId());
        suggestion.setMovementId(movement.getId());
        suggestion.setContaSugerida(contaId);
        suggestion.setConfianca(confianca);
        suggestion.setOrigem(origem);
        return suggestionRepository.save(suggestion);
    }
}
