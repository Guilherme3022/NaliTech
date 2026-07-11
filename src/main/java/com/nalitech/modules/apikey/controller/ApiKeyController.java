package com.nalitech.modules.apikey.controller;

import com.nalitech.modules.apikey.dto.ApiKeyDtos.ApiKeyResponse;
import com.nalitech.modules.apikey.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.nalitech.modules.apikey.dto.ApiKeyDtos.CreatedApiKeyResponse;
import com.nalitech.modules.apikey.service.ApiKeyService;
import com.nalitech.modules.apikey.service.ApiKeyService.CreatedApiKey;
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
@RequestMapping("/api-keys")
@PreAuthorize("hasRole('ADMIN')")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<ApiKeyResponse> list() {
        return apiKeyService.list().stream()
                .map(k -> new ApiKeyResponse(k.getId(), k.getNome(), k.getEscopos(), k.isAtivo(),
                        k.getUltimoUso()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedApiKeyResponse create(@Valid @RequestBody CreateApiKeyRequest request) {
        CreatedApiKey created = apiKeyService.create(request.nome(), request.escopos());
        return new CreatedApiKeyResponse(created.id(), created.nome(), created.escopos(), created.chave());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        apiKeyService.revoke(id);
    }
}
