package com.ledgerflow.modules.fiscal.event;

import java.time.LocalDate;
import java.util.UUID;

public record ObrigacaoVencendoEvent(
        UUID obligationId,
        UUID empresaId,
        UUID clienteId,
        String tipo,
        LocalDate vencimento
) {
}
