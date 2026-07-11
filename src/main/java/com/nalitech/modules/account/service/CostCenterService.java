package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.AccountDtos.CostCenterRequest;
import com.nalitech.modules.account.dto.AccountDtos.CostCenterResponse;
import com.nalitech.modules.account.entity.CostCenter;
import com.nalitech.modules.account.repository.CostCenterRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CostCenterService {

    private final CostCenterRepository repository;

    public CostCenterService(CostCenterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CostCenterResponse> list() {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CostCenterResponse create(CostCenterRequest request) {
        CostCenter costCenter = new CostCenter();
        costCenter.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(costCenter, request);
        return toResponse(repository.save(costCenter));
    }

    public CostCenterResponse update(UUID id, CostCenterRequest request) {
        CostCenter costCenter = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Centro de custo nao encontrado."));
        apply(costCenter, request);
        return toResponse(repository.save(costCenter));
    }

    public void delete(UUID id) {
        CostCenter costCenter = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Centro de custo nao encontrado."));
        repository.delete(costCenter);
    }

    private void apply(CostCenter costCenter, CostCenterRequest request) {
        costCenter.setCodigo(request.codigo());
        costCenter.setNome(request.nome());
        costCenter.setAtivo(request.ativo());
        costCenter.setClienteId(request.clienteId());
    }

    private CostCenterResponse toResponse(CostCenter c) {
        return new CostCenterResponse(c.getId(), c.getCodigo(), c.getNome(), c.isAtivo(), c.getClienteId());
    }
}
