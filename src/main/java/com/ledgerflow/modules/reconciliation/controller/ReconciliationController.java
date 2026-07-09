package com.ledgerflow.modules.reconciliation.controller;

import com.ledgerflow.modules.reconciliation.dto.ReconciliationDtos.ConfirmRequest;
import com.ledgerflow.modules.reconciliation.dto.ReconciliationDtos.ReconciliationResponse;
import com.ledgerflow.modules.reconciliation.entity.ReconciliationStatus;
import com.ledgerflow.modules.reconciliation.service.ReconciliationService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reconciliations")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/pending")
    public Page<ReconciliationResponse> pending(Pageable pageable) {
        return reconciliationService.pending(pageable);
    }

    @GetMapping("/history")
    public Page<ReconciliationResponse> history(
            @RequestParam(defaultValue = "CONFIRMADO") ReconciliationStatus status, Pageable pageable) {
        return reconciliationService.history(status, pageable);
    }

    @PostMapping("/{id}/confirm")
    public ReconciliationResponse confirm(@PathVariable UUID id,
                                          @RequestBody(required = false) ConfirmRequest request) {
        UUID contaSugerida = request == null ? null : request.contaSugerida();
        return reconciliationService.confirm(id, contaSugerida);
    }

    @PostMapping("/{id}/reject")
    public ReconciliationResponse reject(@PathVariable UUID id) {
        return reconciliationService.reject(id);
    }
}
