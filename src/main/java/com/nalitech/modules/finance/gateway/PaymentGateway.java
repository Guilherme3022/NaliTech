package com.nalitech.modules.finance.gateway;

public interface PaymentGateway {

    String provider();

    ChargeModels.ChargeResult criarCobranca(ChargeModels.ChargeRequest request);

    ChargeStatus consultarStatus(String externalId);

    void cancelarCobranca(String externalId);
}
