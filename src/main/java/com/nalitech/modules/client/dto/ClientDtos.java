package com.nalitech.modules.client.dto;

import com.nalitech.modules.client.entity.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ClientDtos {

    private ClientDtos() {
    }

    public record CreateClientRequest(
            @NotBlank String nome,
            @NotBlank String cnpjCpf,
            String contato,
            String telefone,
            @Email String email) {
    }

    public record UpdateClientRequest(
            @NotBlank String nome,
            String contato,
            String telefone,
            @Email String email,
            ClientStatus status) {
    }

    public record ClientResponse(
            UUID id,
            String nome,
            String cnpjCpf,
            String contato,
            String telefone,
            String email,
            ClientStatus status) {
    }

    public record ClientDocumentResponse(
            UUID id,
            UUID fileId,
            String descricao,
            OffsetDateTime createdAt) {
    }
}
