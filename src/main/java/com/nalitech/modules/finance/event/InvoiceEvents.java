package com.nalitech.modules.finance.event;

import java.math.BigDecimal;
import java.util.UUID;

public final class InvoiceEvents {

    private InvoiceEvents() {
    }

    public record InvoiceCreatedEvent(UUID invoiceId, UUID empresaId, UUID clienteId,
                                      BigDecimal valor, String boletoUrl) {
    }

    public record InvoicePaidEvent(UUID invoiceId, UUID empresaId, UUID clienteId, BigDecimal valor) {
    }

    public record InvoiceOverdueEvent(UUID invoiceId, UUID empresaId, UUID clienteId,
                                      BigDecimal valor, long diasEmAtraso) {
    }
}
