package com.ledgerflow.modules.finance.dto;

import com.ledgerflow.modules.finance.entity.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceDtos {

    private FinanceDtos() {
    }

    public record CreateFeeRequest(
            @NotNull UUID clienteId,
            String descricao,
            @NotNull @Positive BigDecimal valor,
            String periodicidade) {
    }

    public record FeeResponse(UUID id, UUID clienteId, String descricao, BigDecimal valor,
                              String periodicidade, boolean ativo) {
    }

    public record CreateInvoiceRequest(
            @NotNull UUID clienteId,
            UUID feeId,
            @NotNull @Positive BigDecimal valor,
            @NotNull LocalDate vencimento,
            String descricao) {
    }

    public record InvoiceResponse(UUID id, UUID clienteId, BigDecimal valor, LocalDate vencimento,
                                  InvoiceStatus status, String provider, String externalId,
                                  String boletoUrl, String pixCopiaCola) {
    }
}
