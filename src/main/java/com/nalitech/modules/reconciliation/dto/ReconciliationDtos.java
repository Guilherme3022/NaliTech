package com.nalitech.modules.reconciliation.dto;

import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ReconciliationDtos {

    private ReconciliationDtos() {
    }

    public record ReconciliationResponse(
            UUID id,
            UUID clienteId,
            LocalDate competencia,
            UUID movementId,
            UUID matchedMovementId,
            ReconciliationStatus status,
            String camada,
            BigDecimal score,
            String motivo) {
    }

    public record ConfirmRequest(UUID contaSugerida) {
    }
}
