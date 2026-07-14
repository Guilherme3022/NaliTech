package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationResponse;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoConfirmadaEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final MovementRepository movementRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReconciliationService(ReconciliationRepository reconciliationRepository,
                                 MovementRepository movementRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationResponse> pending(UUID clienteId, LocalDate competencia, Pageable pageable) {
        return reconciliationRepository
                .search(SecurityUtils.currentEmpresaId(), ReconciliationStatus.PENDENTE,
                        clienteId, competencia, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationResponse> history(ReconciliationStatus status, UUID clienteId,
                                                LocalDate competencia, Pageable pageable) {
        return reconciliationRepository
                .search(SecurityUtils.currentEmpresaId(), status, clienteId, competencia, pageable)
                .map(this::toResponse);
    }

    public ReconciliationResponse confirm(UUID id, UUID contaSugerida) {
        Reconciliation reconciliation = findPending(id);
        reconciliation.setStatus(ReconciliationStatus.CONFIRMADO);
        reconciliationRepository.save(reconciliation);

        updateMovementStatus(reconciliation.getMovementId(), MovementStatus.CONCILIADO);

        eventPublisher.publishEvent(new ConciliacaoConfirmadaEvent(
                reconciliation.getId(), reconciliation.getEmpresaId(),
                reconciliation.getMovementId(), contaSugerida));
        return toResponse(reconciliation);
    }

    public ReconciliationResponse reject(UUID id) {
        Reconciliation reconciliation = findPending(id);
        reconciliation.setStatus(ReconciliationStatus.REJEITADO);
        reconciliationRepository.save(reconciliation);
        updateMovementStatus(reconciliation.getMovementId(), MovementStatus.NORMALIZADO);
        return toResponse(reconciliation);
    }

    private Reconciliation findPending(UUID id) {
        return reconciliationRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conciliacao nao encontrada."));
    }

    private void updateMovementStatus(UUID movementId, MovementStatus status) {
        movementRepository.findById(movementId).ifPresent(movement -> {
            movement.setStatus(status);
            movementRepository.save(movement);
        });
    }

    private ReconciliationResponse toResponse(Reconciliation r) {
        return new ReconciliationResponse(r.getId(), r.getClienteId(), r.getCompetencia(),
                r.getMovementId(), r.getMatchedMovementId(),
                r.getStatus(), r.getCamada(), r.getScore(), r.getMotivo());
    }
}
