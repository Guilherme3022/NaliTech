package com.nalitech.modules.account.service;

import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoConfirmadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LearningEventListener {

    private final MovementRepository movementRepository;
    private final LearningService learningService;

    public LearningEventListener(MovementRepository movementRepository,
                                 LearningService learningService) {
        this.movementRepository = movementRepository;
        this.learningService = learningService;
    }

    @EventListener
    @Transactional
    public void onConciliacaoConfirmada(ConciliacaoConfirmadaEvent event) {
        if (event.contaSugerida() == null) {
            return;
        }
        movementRepository.findById(event.movementId()).ifPresent(movement ->
                learningService.recordDecision(
                        event.empresaId(), movement.getClienteId(),
                        movement.getDescricao(), event.contaSugerida()));
    }
}
