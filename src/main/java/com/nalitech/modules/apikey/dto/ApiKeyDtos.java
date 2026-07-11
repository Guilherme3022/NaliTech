package com.nalitech.modules.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ApiKeyDtos {

    private ApiKeyDtos() {
    }

    public record CreateApiKeyRequest(
            @NotBlank String nome,
            String escopos) {
    }

    public record CreatedApiKeyResponse(UUID id, String nome, String escopos, String chave) {
    }

    public record ApiKeyResponse(UUID id, String nome, String escopos, boolean ativo,
                                 OffsetDateTime ultimoUso) {
    }
}
