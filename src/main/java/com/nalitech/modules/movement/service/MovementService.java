package com.nalitech.modules.movement.service;

import com.nalitech.modules.movement.dto.MovementDtos.MovementResponse;
import com.nalitech.modules.movement.dto.MovementDtos.UpdateMovementRequest;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MovementService {

    private final MovementRepository movementRepository;
    private final ReconciliationRepository reconciliationRepository;

    public MovementService(MovementRepository movementRepository,
                           ReconciliationRepository reconciliationRepository) {
        this.movementRepository = movementRepository;
        this.reconciliationRepository = reconciliationRepository;
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> list(UUID clienteId, LocalDate competencia, Pageable pageable) {
        LocalDate inicio = competencia;
        LocalDate fim = competencia == null ? null : competencia.plusMonths(1).minusDays(1);
        return movementRepository
                .search(SecurityUtils.currentEmpresaId(), clienteId, inicio, fim, pageable)
                .map(this::toResponse);
    }

    public MovementResponse update(UUID id, UpdateMovementRequest request) {
        Movement movement = find(id);
        if (request.data() != null) {
            movement.setData(request.data());
        }
        if (request.valor() != null) {
            movement.setValor(request.valor());
        }
        movement.setDescricao(request.descricao());
        movement.setDocumento(request.documento());
        movement.setContaDebitoId(request.contaDebitoId());
        movement.setContaCreditoId(request.contaCreditoId());
        return toResponse(movementRepository.save(movement));
    }

    public void delete(UUID id) {
        Movement movement = find(id);
        // Remove tambem os itens de conciliacao gerados a partir desta movimentacao.
        reconciliationRepository.deleteByMovementIdIn(List.of(movement.getId()));
        movementRepository.delete(movement);
    }

    private Movement find(UUID id) {
        return movementRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));
    }

    private MovementResponse toResponse(Movement m) {
        return new MovementResponse(m.getId(), m.getClienteId(), m.getData(), m.getValor(),
                m.getDescricao(), m.getTipo(), m.getDocumento(), m.getBanco(),
                m.getContaDebitoId(), m.getContaCreditoId(), m.getStatus());
    }
}
