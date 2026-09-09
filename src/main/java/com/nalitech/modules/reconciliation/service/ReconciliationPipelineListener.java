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
        // Chaves (cliente, competencia) tocadas, para a otimizacao global depois.
        java.util.Set<java.util.Map.Entry<java.util.UUID, java.time.LocalDate>> escopos =
                new java.util.HashSet<>();
        for (var movementId : event.movementIds()) {
            movementRepository.findById(movementId).ifPresent(movement -> {
                try {
                    matchingService.reconcile(movement);
                    if (movement.getClienteId() != null && movement.getData() != null) {
                        escopos.add(java.util.Map.entry(
                                movement.getClienteId(), movement.getData().withDayOfMonth(1)));
                    }
                } catch (Exception ex) {
                    log.error("Falha ao conciliar movimentacao {}", movementId, ex);
                }
            });
        }
        // Passo global: melhora o match considerando todos juntos (best-effort).
        for (var escopo : escopos) {
            try {
                matchingService.optimize(event.empresaId(), escopo.getKey(), escopo.getValue());
            } catch (Exception ex) {
                log.error("Falha ao otimizar conciliacao do cliente {} competencia {}",
                        escopo.getKey(), escopo.getValue(), ex);
            }
        }
    }
}
