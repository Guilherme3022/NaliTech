package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationResponse;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoConfirmadaEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReconciliationService(ReconciliationRepository reconciliationRepository,
                                 MovementRepository movementRepository,
                                 ChartOfAccountRepository chartOfAccountRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
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
        requirePlanoDeContas(reconciliation);
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

    // EB (spec 7/12): nao permite concluir conciliacao se o cliente nao tem plano ativo.
    private void requirePlanoDeContas(Reconciliation reconciliation) {
        UUID clienteId = reconciliation.getClienteId();
        if (clienteId == null) {
            return; // conciliacoes legadas sem cliente nao sao bloqueadas
        }
        boolean temPlano = chartOfAccountRepository.existsPlanoForCliente(
                reconciliation.getEmpresaId(), clienteId);
        if (!temPlano) {
            throw new BusinessException(
                    "Nao foi identificado um plano de contas ativo para este cliente. "
                    + "Configure ou vincule um plano de contas antes de iniciar a conciliacao.",
                    HttpStatus.BAD_REQUEST);
        }
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
