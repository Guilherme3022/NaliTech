package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.AccountDtos.LoanContractRequest;
import com.nalitech.modules.account.dto.AccountDtos.LoanContractResponse;
import com.nalitech.modules.account.service.LoanContractService;
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
@RequestMapping("/loan-contracts")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class LoanContractController {

    private final LoanContractService loanContractService;

    public LoanContractController(LoanContractService loanContractService) {
        this.loanContractService = loanContractService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
    public List<LoanContractResponse> list() {
        return loanContractService.list();
    }

    @PostMapping
    public LoanContractResponse create(@Valid @RequestBody LoanContractRequest request) {
        return loanContractService.create(request);
    }

    @PutMapping("/{id}")
    public LoanContractResponse update(@PathVariable UUID id, @Valid @RequestBody LoanContractRequest request) {
        return loanContractService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        loanContractService.delete(id);
    }
}
