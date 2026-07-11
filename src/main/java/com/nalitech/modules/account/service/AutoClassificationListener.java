package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.AiSuggestion;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoConfirmadaEvent;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classificacao automatica ("De/Para automatico"): assim que uma movimentacao e
 * conciliada, tenta aplicar a conta contabil sozinho. Usa a conta ja sugerida na
 * conciliacao ou, na falta dela, o motor de sugestao (regra + historico de
 * aprendizado). So aplica automaticamente quando a confianca e alta; o restante
 * fica na fila de "Solicitacao de Parametrizacao" para revisao humana.
 *
 * Efeito pratico: quanto mais o contador parametriza, mais o sistema classifica
 * sozinho nos meses seguintes (aprendizado retroalimentado).
 */
@Slf4j
@Component
public class AutoClassificationListener {

    /** Confianca minima (0-100) para classificar sem intervencao humana. */
    private static final int AUTO_THRESHOLD = 90;

    private final MovementRepository movementRepository;
    private final ClassificationSuggestionService suggestionService;
    private final ClassificationService classificationService;

    public AutoClassificationListener(MovementRepository movementRepository,
                                      ClassificationSuggestionService suggestionService,
                                      ClassificationService classificationService) {
        this.movementRepository = movementRepository;
        this.suggestionService = suggestionService;
        this.classificationService = classificationService;
    }

    @EventListener
    @Transactional
    public void onConciliacaoConfirmada(ConciliacaoConfirmadaEvent event) {
        Movement movement = movementRepository.findById(event.movementId()).orElse(null);
        if (movement == null
                || movement.getCategoriaSugerida() != null
                || movement.getStatus() == MovementStatus.CLASSIFICADO) {
            return; // ja classificado ou inexistente
        }

        UUID conta = event.contaSugerida();
        if (conta == null) {
            conta = highConfidenceSuggestion(event.movementId());
        }

        if (conta != null) {
            classificationService.classify(event.movementId(), conta);
            log.debug("Movimentacao {} classificada automaticamente na conta {}",
                    event.movementId(), conta);
        }
    }

    private UUID highConfidenceSuggestion(UUID movementId) {
        AiSuggestion suggestion = suggestionService.suggestFor(movementId);
        boolean confiavel = suggestion.getContaSugerida() != null
                && suggestion.getConfianca() != null
                && suggestion.getConfianca().intValue() >= AUTO_THRESHOLD;
        return confiavel ? suggestion.getContaSugerida() : null;
    }
}
