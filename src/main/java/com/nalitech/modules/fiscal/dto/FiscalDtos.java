package com.nalitech.modules.fiscal.dto;

import com.nalitech.modules.fiscal.entity.ObligationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public final class FiscalDtos {

    private FiscalDtos() {
    }

    public record ObligationRequest(
            UUID clienteId,
            @NotBlank String tipo,
            String descricao,
            @NotNull LocalDate vencimento,
            ObligationStatus status) {
    }

    public record ObligationResponse(
            UUID id, UUID clienteId, String tipo, String descricao,
            LocalDate vencimento, ObligationStatus status) {
    }
}
