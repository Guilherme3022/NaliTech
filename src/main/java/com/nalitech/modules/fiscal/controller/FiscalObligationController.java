package com.nalitech.modules.fiscal.controller;

import com.nalitech.modules.fiscal.dto.FiscalDtos.ObligationRequest;
import com.nalitech.modules.fiscal.dto.FiscalDtos.ObligationResponse;
import com.nalitech.modules.fiscal.service.FiscalObligationService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/fiscal-obligations")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class FiscalObligationController {

    private final FiscalObligationService service;

    public FiscalObligationController(FiscalObligationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ObligationResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/upcoming")
    public List<ObligationResponse> upcoming(@RequestParam(defaultValue = "7") int dias) {
        return service.upcoming(dias);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObligationResponse create(@Valid @RequestBody ObligationRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ObligationResponse update(@PathVariable UUID id, @Valid @RequestBody ObligationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
