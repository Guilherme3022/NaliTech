package com.ledgerflow.modules.finance.gateway;

import com.ledgerflow.modules.finance.gateway.ChargeModels.ChargeRequest;
import com.ledgerflow.modules.finance.gateway.ChargeModels.ChargeResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public String provider() {
        return "SIMULADO";
    }

    @Override
    public ChargeResult criarCobranca(ChargeRequest request) {
        String externalId = "sim_" + UUID.randomUUID();
        return new ChargeResult(
                externalId,
                "https://sandbox.local/boleto/" + externalId,
                "00020126PIX-COPIA-E-COLA-" + externalId,
                "iVBORw0KGgo-qrcode-fake",
                ChargeStatus.PENDENTE);
    }

    @Override
    public ChargeStatus consultarStatus(String externalId) {
        return ChargeStatus.PENDENTE;
    }

    @Override
    public void cancelarCobranca(String externalId) {

    }
}
