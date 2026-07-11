package com.nalitech.modules.fiscal.service;

import com.nalitech.modules.fiscal.dto.FiscalDtos.ObligationRequest;
import com.nalitech.modules.fiscal.dto.FiscalDtos.ObligationResponse;
import com.nalitech.modules.fiscal.entity.FiscalObligation;
import com.nalitech.modules.fiscal.entity.ObligationStatus;
import com.nalitech.modules.fiscal.repository.FiscalObligationRepository;
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
public class FiscalObligationService {

    private final FiscalObligationRepository repository;

    public FiscalObligationService(FiscalObligationRepository repository) {
        this.repository = repository;
    }

    public ObligationResponse create(ObligationRequest request) {
        FiscalObligation obligation = new FiscalObligation();
        obligation.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(obligation, request);
        return toResponse(repository.save(obligation));
    }

    @Transactional(readOnly = true)
    public Page<ObligationResponse> list(Pageable pageable) {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId(), pageable)
                .map(this::toResponse);
    }

    public ObligationResponse update(UUID id, ObligationRequest request) {
        FiscalObligation obligation = findInCurrentCompany(id);
        apply(obligation, request);
        return toResponse(repository.save(obligation));
    }

    public void delete(UUID id) {
        repository.delete(findInCurrentCompany(id));
    }

    @Transactional(readOnly = true)
    public List<ObligationResponse> upcoming(int dias) {
        LocalDate hoje = LocalDate.now();
        return repository.findByEmpresaIdAndStatusAndVencimentoBetween(
                        SecurityUtils.currentEmpresaId(), ObligationStatus.PENDENTE, hoje, hoje.plusDays(dias))
                .stream().map(this::toResponse).toList();
    }

    private FiscalObligation findInCurrentCompany(UUID id) {
        return repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Obrigacao nao encontrada."));
    }

    private void apply(FiscalObligation obligation, ObligationRequest request) {
        obligation.setClienteId(request.clienteId());
        obligation.setTipo(request.tipo());
        obligation.setDescricao(request.descricao());
        obligation.setVencimento(request.vencimento());
        if (request.status() != null) {
            obligation.setStatus(request.status());
        }
    }

    private ObligationResponse toResponse(FiscalObligation o) {
        return new ObligationResponse(o.getId(), o.getClienteId(), o.getTipo(),
                o.getDescricao(), o.getVencimento(), o.getStatus());
    }
}
