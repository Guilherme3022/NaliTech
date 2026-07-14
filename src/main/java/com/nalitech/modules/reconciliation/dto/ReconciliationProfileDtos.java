package com.nalitech.modules.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class ReconciliationProfileDtos {

    private ReconciliationProfileDtos() {
    }

    public record ReconciliationProfileResponse(
            UUID id,
            UUID clienteId,
            String nome,
            String sistemaOrigem,
            String tipoArquivo,
            String sistemaContabilDestino,
            UUID planoId,
            boolean ativo) {
    }

    public record ReconciliationProfileRequest(
            @NotNull UUID clienteId,
            @NotBlank String nome,
            String sistemaOrigem,
            String tipoArquivo,
            String sistemaContabilDestino,
            UUID planoId,
            Boolean ativo) {
    }
}
