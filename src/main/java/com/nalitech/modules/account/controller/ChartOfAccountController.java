package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.AccountDtos.ChartAccountRequest;
import com.nalitech.modules.account.dto.AccountDtos.ChartAccountResponse;
import com.nalitech.modules.account.service.AccountService;
import com.nalitech.modules.account.service.ChartImportService;
import com.nalitech.modules.account.service.ChartImportService.ImportResult;
import com.nalitech.shared.exception.BusinessException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/chart-of-accounts")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class ChartOfAccountController {

    private final AccountService accountService;
    private final ChartImportService chartImportService;

    public ChartOfAccountController(AccountService accountService,
                                   ChartImportService chartImportService) {
        this.accountService = accountService;
        this.chartImportService = chartImportService;
    }

    @GetMapping
    public Page<ChartAccountResponse> list(Pageable pageable) {
        return accountService.listAccounts(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChartAccountResponse create(@Valid @RequestBody ChartAccountRequest request) {
        return accountService.createAccount(request);
    }

    @PutMapping("/{id}")
    public ChartAccountResponse update(@PathVariable UUID id, @Valid @RequestBody ChartAccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        accountService.deleteAccount(id);
    }

    // Importa plano de contas de Excel (.xlsx/.xls) ou CSV para um cliente.
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ImportResult importChart(@RequestParam("file") MultipartFile file,
                                    @RequestParam("clienteId") UUID clienteId) {
        try {
            return chartImportService.importChart(
                    clienteId, file.getOriginalFilename(), file.getBytes());
        } catch (IOException ex) {
            throw new BusinessException("Falha ao ler o arquivo enviado.", HttpStatus.BAD_REQUEST);
        }
    }
}
