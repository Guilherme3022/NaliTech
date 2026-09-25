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
        // Aprende a PARTIDA DOBRADA (debito + credito) ja gravada no movimento pela confirmacao,
        // por descricao e por CNPJ, para pre-preencher as duas contas na proxima vez.
        movementRepository.findById(event.movementId()).ifPresent(movement ->
                learningService.recordDecision(
                        event.empresaId(), movement.getClienteId(),
                        movement.getDescricao(), movement.getDocumento(),
                        movement.getContaDebitoId(), movement.getContaCreditoId()));
    }
}
