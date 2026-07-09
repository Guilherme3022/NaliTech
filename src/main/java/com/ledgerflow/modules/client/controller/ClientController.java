package com.ledgerflow.modules.client.controller;

import com.ledgerflow.modules.client.dto.ClientDtos.ClientDocumentResponse;
import com.ledgerflow.modules.client.dto.ClientDtos.ClientResponse;
import com.ledgerflow.modules.client.dto.ClientDtos.CreateClientRequest;
import com.ledgerflow.modules.client.dto.ClientDtos.UpdateClientRequest;
import com.ledgerflow.modules.client.service.ClientService;
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
@RequestMapping("/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public Page<ClientResponse> list(@RequestParam(required = false) String search, Pageable pageable) {
        return clientService.search(search, pageable);
    }

    @GetMapping("/{id}")
    public ClientResponse getById(@PathVariable UUID id) {
        return clientService.getById(id);
    }

    @GetMapping("/{id}/documents")
    public List<ClientDocumentResponse> documents(@PathVariable UUID id) {
        return clientService.listDocuments(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClientRequest request) {
        return clientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        clientService.delete(id);
    }
}
