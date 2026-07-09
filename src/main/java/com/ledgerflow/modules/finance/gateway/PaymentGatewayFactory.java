package com.ledgerflow.modules.finance.gateway;

import com.ledgerflow.shared.exception.BusinessException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {

    private final List<PaymentGateway> gateways;
    private final String defaultProvider;

    public PaymentGatewayFactory(List<PaymentGateway> gateways,
                                 @Value("${PAYMENT_PROVIDER:SIMULADO}") String defaultProvider) {
        this.gateways = gateways;
        this.defaultProvider = defaultProvider;
    }

    public PaymentGateway active() {
        return resolve(defaultProvider);
    }

    public PaymentGateway resolve(String provider) {
        return gateways.stream()
                .filter(gateway -> gateway.provider().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Provedor de pagamento nao suportado: " + provider, HttpStatus.BAD_REQUEST));
    }
}
