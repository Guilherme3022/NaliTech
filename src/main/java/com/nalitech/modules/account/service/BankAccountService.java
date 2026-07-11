package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.AccountDtos.BankAccountRequest;
import com.nalitech.modules.account.dto.AccountDtos.BankAccountResponse;
import com.nalitech.modules.account.entity.BankAccount;
import com.nalitech.modules.account.repository.BankAccountRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BankAccountService {

    private final BankAccountRepository repository;

    public BankAccountService(BankAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> list() {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public BankAccountResponse create(BankAccountRequest request) {
        BankAccount account = new BankAccount();
        account.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(account, request);
        return toResponse(repository.save(account));
    }

    public BankAccountResponse update(UUID id, BankAccountRequest request) {
        BankAccount account = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancaria nao encontrada."));
        apply(account, request);
        return toResponse(repository.save(account));
    }

    public void delete(UUID id) {
        BankAccount account = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancaria nao encontrada."));
        repository.delete(account);
    }

    private void apply(BankAccount account, BankAccountRequest request) {
        account.setNome(request.nome());
        account.setContaContabilId(request.contaContabilId());
        account.setPadrao(request.padrao());
        account.setClienteId(request.clienteId());
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(), account.getNome(), account.getContaContabilId(),
                account.isPadrao(), account.getClienteId());
    }
}
