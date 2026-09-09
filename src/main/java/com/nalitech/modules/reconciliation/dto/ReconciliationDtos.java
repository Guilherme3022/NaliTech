package com.nalitech.modules.reconciliation.dto;

import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    /** Resultado do reprocessamento de pendencias sem correspondencia (MANUAL). */
    public record ReprocessResponse(int reprocessados, int resolvidos) {
    }

    /** Estado de um job de varredura por IA (para a barra de progresso / popup). */
    public record AiSweepJob(
            UUID jobId,
            String status,          // EXECUTANDO, CONCLUIDO, ERRO, SEM_PENDENCIAS
            int total,              // pendencias a analisar
            int processados,        // ja avaliadas pela IA
            int resolvidos,         // quantas a IA conseguiu conciliar
            UUID clienteId,
            LocalDate competencia,
            OffsetDateTime iniciadoEm,
            OffsetDateTime concluidoEm) {
    }
}
