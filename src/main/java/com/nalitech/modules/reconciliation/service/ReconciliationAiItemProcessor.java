package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher.AiMatch;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa UM item da varredura por IA numa transacao propria, para que o
 * progresso do sweep seja duravel e a falha de um item nao derrube os demais.
 * Vive num bean separado do {@link ReconciliationAiSweepService} de proposito:
 * assim o {@code @Transactional} por item passa pelo proxy do Spring (chamada
 * entre beans), o que nao aconteceria com auto-invocacao.
 */
@Service
public class ReconciliationAiItemProcessor {

    private final ReconciliationRepository reconciliationRepository;
    private final MovementRepository movementRepository;
    private final MatchingService matchingService;

    public ReconciliationAiItemProcessor(ReconciliationRepository reconciliationRepository,
                                         MovementRepository movementRepository,
                                         MatchingService matchingService) {
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
        this.matchingService = matchingService;
    }

    /**
     * Avalia uma pendencia MANUAL com a IA. Marca {@code iaTentada=true} de todo
     * jeito (memoria: nao chamar de novo). Devolve true se a IA conciliou o item.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(UUID reconciliationId, UUID empresaId) {
        Reconciliation r = reconciliationRepository.findByIdAndEmpresaId(reconciliationId, empresaId)
                .orElse(null);
        if (r == null || r.getStatus() != ReconciliationStatus.PENDENTE
                || !"MANUAL".equals(r.getCamada()) || r.isIaTentada()) {
            return false;
        }
        Movement movement = movementRepository.findById(r.getMovementId()).orElse(null);
        if (movement == null) {
            r.setIaTentada(true);
            reconciliationRepository.save(r);
            return false;
        }

        Optional<AiMatch> aiMatch = matchingService.evaluateWithAi(movement);
        boolean resolvido = false;
        if (aiMatch.isPresent()) {
            AiMatch match = aiMatch.get();
            r.setCamada("IA");
            r.setMatchedMovementId(match.matchedMovementId());
            r.setScore(match.confianca());
            r.setMotivo("IA: " + match.justificativa());
            resolvido = true;
        }
        r.setIaTentada(true);
        reconciliationRepository.save(r);
        return resolvido;
    }
}
