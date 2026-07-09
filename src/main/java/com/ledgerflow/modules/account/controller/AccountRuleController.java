package com.ledgerflow.modules.account.controller;

import com.ledgerflow.modules.account.dto.AccountDtos.AccountRuleRequest;
import com.ledgerflow.modules.account.dto.AccountDtos.AccountRuleResponse;
import com.ledgerflow.modules.account.service.AccountService;
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
@RequestMapping("/account-rules")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class AccountRuleController {

    private final AccountService accountService;

    public AccountRuleController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountRuleResponse> list() {
        return accountService.listRules();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountRuleResponse create(@Valid @RequestBody AccountRuleRequest request) {
        return accountService.createRule(request);
    }

    @PutMapping("/{id}")
    public AccountRuleResponse update(@PathVariable UUID id, @Valid @RequestBody AccountRuleRequest request) {
        return accountService.updateRule(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        accountService.deleteRule(id);
    }
}
