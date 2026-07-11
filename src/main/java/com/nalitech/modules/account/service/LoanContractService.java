package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.AccountDtos.LoanContractRequest;
import com.nalitech.modules.account.dto.AccountDtos.LoanContractResponse;
import com.nalitech.modules.account.entity.LoanContract;
import com.nalitech.modules.account.repository.LoanContractRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoanContractService {

    private final LoanContractRepository repository;

    public LoanContractService(LoanContractRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LoanContractResponse> list() {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public LoanContractResponse create(LoanContractRequest request) {
        LoanContract contract = new LoanContract();
        contract.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(contract, request);
        return toResponse(repository.save(contract));
    }

    public LoanContractResponse update(UUID id, LoanContractRequest request) {
        LoanContract contract = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato nao encontrado."));
        apply(contract, request);
        return toResponse(repository.save(contract));
    }

    public void delete(UUID id) {
        LoanContract contract = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato nao encontrado."));
        repository.delete(contract);
    }

    private void apply(LoanContract contract, LoanContractRequest request) {
        contract.setDescricao(request.descricao());
        contract.setValorTotal(request.valorTotal());
        contract.setTaxaJuros(request.taxaJuros());
        contract.setParcelas(request.parcelas());
        contract.setContaPrincipalId(request.contaPrincipalId());
        contract.setContaJurosId(request.contaJurosId());
        contract.setContaEncargosId(request.contaEncargosId());
        contract.setClassificacaoPrazo(request.classificacaoPrazo());
        contract.setAtivo(request.ativo());
        contract.setClienteId(request.clienteId());
    }

    private LoanContractResponse toResponse(LoanContract c) {
        return new LoanContractResponse(c.getId(), c.getDescricao(), c.getValorTotal(), c.getTaxaJuros(),
                c.getParcelas(), c.getContaPrincipalId(), c.getContaJurosId(), c.getContaEncargosId(),
                c.getClassificacaoPrazo(), c.isAtivo(), c.getClienteId());
    }
}
