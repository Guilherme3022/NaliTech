package com.nalitech.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class PlanoModeloDtos {

    private PlanoModeloDtos() {
    }

    public record ContaResponse(UUID id, String codigo, String nome, String tipo) {
    }

    public record PlanoModeloResponse(
            UUID id, String nome, String descricao, List<ContaResponse> contas) {
    }

    public record CreatePlanoModeloRequest(
            @NotBlank String nome,
            String descricao) {
    }

    public record ContaRequest(
            @NotBlank String codigo,
            @NotBlank String nome,
            String tipo) {
    }

    public record AplicarModeloRequest(
            @NotNull UUID clienteId) {
    }

    public record AplicarModeloResponse(int contasCriadas, int contasIgnoradas) {
    }
}
