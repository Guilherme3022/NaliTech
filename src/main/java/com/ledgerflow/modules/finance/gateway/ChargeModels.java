package com.ledgerflow.modules.finance.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ChargeModels {

    private ChargeModels() {
    }

    public record ChargeRequest(
            String clienteNome,
            String clienteDocumento,
            BigDecimal valor,
            LocalDate vencimento,
            String descricao) {
    }

    public record ChargeResult(
            String externalId,
            String boletoUrl,
            String pixCopiaECola,
            String pixQrCode,
            ChargeStatus status) {
    }
}
