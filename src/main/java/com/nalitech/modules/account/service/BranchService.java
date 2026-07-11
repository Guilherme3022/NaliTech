package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.AccountDtos.BranchRequest;
import com.nalitech.modules.account.dto.AccountDtos.BranchResponse;
import com.nalitech.modules.account.entity.Branch;
import com.nalitech.modules.account.repository.BranchRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BranchService {

    private final BranchRepository repository;

    public BranchService(BranchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> list() {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse create(BranchRequest request) {
        Branch branch = new Branch();
        branch.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(branch, request);
        return toResponse(repository.save(branch));
    }

    public BranchResponse update(UUID id, BranchRequest request) {
        Branch branch = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial nao encontrada."));
        apply(branch, request);
        return toResponse(repository.save(branch));
    }

    public void delete(UUID id) {
        Branch branch = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial nao encontrada."));
        repository.delete(branch);
    }

    private void apply(Branch branch, BranchRequest request) {
        branch.setCodigo(request.codigo());
        branch.setNome(request.nome());
        branch.setCnpj(request.cnpj());
        branch.setAtivo(request.ativo());
        branch.setClienteId(request.clienteId());
    }

    private BranchResponse toResponse(Branch b) {
        return new BranchResponse(b.getId(), b.getCodigo(), b.getNome(), b.getCnpj(),
                b.isAtivo(), b.getClienteId());
    }
}
