package com.nalitech.modules.reconciliation.event;

import java.time.LocalDate;
import java.util.UUID;

public final class ConciliacaoEvents {

    private ConciliacaoEvents() {
    }

    /**
     * Varredura por IA concluida. Evento interno de dominio (nao usa n8n): pode
     * alimentar notificacao nativa por e-mail, metricas ou auditoria.
     */
    public record SweepIaConcluidoEvent(UUID empresaId, UUID jobId, UUID clienteId,
                                        LocalDate competencia, int total, int resolvidos) {
    }

    public record ConciliacaoPendenteEvent(UUID reconciliationId, UUID empresaId,
                                           UUID movementId, String motivo) {
    }

    public record ConciliacaoConfirmadaEvent(UUID reconciliationId, UUID empresaId,
                                             UUID movementId, UUID contaSugerida) {
    }
}
