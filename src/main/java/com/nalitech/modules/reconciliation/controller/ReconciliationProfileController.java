package com.nalitech.modules.reconciliation.controller;

import com.nalitech.modules.reconciliation.dto.ReconciliationProfileDtos.ReconciliationProfileRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationProfileDtos.ReconciliationProfileResponse;
import com.nalitech.modules.reconciliation.service.ReconciliationProfileService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reconciliation-profiles")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class ReconciliationProfileController {

    private final ReconciliationProfileService service;

    public ReconciliationProfileController(ReconciliationProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReconciliationProfileResponse> list(@RequestParam(required = false) UUID clienteId) {
        return service.list(clienteId);
    }

    @GetMapping("/{id}")
    public ReconciliationProfileResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReconciliationProfileResponse create(@Valid @RequestBody ReconciliationProfileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ReconciliationProfileResponse update(@PathVariable UUID id,
                                                @Valid @RequestBody ReconciliationProfileRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
