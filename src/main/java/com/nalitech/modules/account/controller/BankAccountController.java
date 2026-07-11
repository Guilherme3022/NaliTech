package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.AccountDtos.BankAccountRequest;
import com.nalitech.modules.account.dto.AccountDtos.BankAccountResponse;
import com.nalitech.modules.account.service.BankAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bank-accounts")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    public List<BankAccountResponse> list() {
        return bankAccountService.list();
    }

    @PostMapping
    public BankAccountResponse create(@Valid @RequestBody BankAccountRequest request) {
        return bankAccountService.create(request);
    }

    @PutMapping("/{id}")
    public BankAccountResponse update(@PathVariable UUID id, @Valid @RequestBody BankAccountRequest request) {
        return bankAccountService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        bankAccountService.delete(id);
    }
}
