package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.PlanoModeloDtos.AplicarModeloRequest;
import com.nalitech.modules.account.dto.PlanoModeloDtos.AplicarModeloResponse;
import com.nalitech.modules.account.dto.PlanoModeloDtos.ContaRequest;
import com.nalitech.modules.account.dto.PlanoModeloDtos.CreatePlanoModeloRequest;
import com.nalitech.modules.account.dto.PlanoModeloDtos.PlanoModeloResponse;
import com.nalitech.modules.account.service.PlanoModeloService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plano-modelos")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class PlanoModeloController {

    private final PlanoModeloService service;

    public PlanoModeloController(PlanoModeloService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlanoModeloResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public PlanoModeloResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoModeloResponse create(@Valid @RequestBody CreatePlanoModeloRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/contas")
    public PlanoModeloResponse addConta(@PathVariable UUID id, @Valid @RequestBody ContaRequest request) {
        return service.addConta(id, request);
    }

    @PostMapping("/{id}/aplicar")
    public AplicarModeloResponse aplicar(@PathVariable UUID id,
                                         @Valid @RequestBody AplicarModeloRequest request) {
        return service.aplicar(id, request.clienteId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
