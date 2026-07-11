package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.event.MovimentacoesNormalizadasEvent;
import com.nalitech.modules.movement.repository.MovementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ReconciliationPipelineListener {

    private final MatchingService matchingService;
    private final MovementRepository movementRepository;

    public ReconciliationPipelineListener(MatchingService matchingService,
                                          MovementRepository movementRepository) {
        this.matchingService = matchingService;
        this.movementRepository = movementRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovimentacoesNormalizadas(MovimentacoesNormalizadasEvent event) {
        for (var movementId : event.movementIds()) {
            movementRepository.findById(movementId).ifPresent(movement -> {
                try {
                    matchingService.reconcile(movement);
                } catch (Exception ex) {
                    log.error("Falha ao conciliar movimentacao {}", movementId, ex);
                }
            });
        }
    }
}
