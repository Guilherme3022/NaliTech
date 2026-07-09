package com.ledgerflow.modules.reconciliation.event;

import java.util.UUID;

public final class ConciliacaoEvents {

    private ConciliacaoEvents() {
    }

    public record ConciliacaoPendenteEvent(UUID reconciliationId, UUID empresaId,
                                           UUID movementId, String motivo) {
    }

    public record ConciliacaoConfirmadaEvent(UUID reconciliationId, UUID empresaId,
                                             UUID movementId, UUID contaSugerida) {
    }
}
