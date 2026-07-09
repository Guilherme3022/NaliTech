package com.ledgerflow.modules.finance.gateway;

import com.ledgerflow.modules.finance.gateway.ChargeModels.ChargeRequest;
import com.ledgerflow.modules.finance.gateway.ChargeModels.ChargeResult;
import com.ledgerflow.shared.exception.BusinessException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AsaasPaymentGateway implements PaymentGateway {

    private final RestClient restClient;

    public AsaasPaymentGateway(@Value("${ASAAS_BASE_URL:https://sandbox.asaas.com/api/v3}") String baseUrl,
                               @Value("${ASAAS_API_TOKEN:}") String apiToken) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("access_token", apiToken)
                .build();
    }

    @Override
    public String provider() {
        return "ASAAS";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChargeResult criarCobranca(ChargeRequest request) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/payments")
                    .body(Map.of(
                            "billingType", "PIX",
                            "value", request.valor(),
                            "dueDate", request.vencimento().toString(),
                            "description", request.descricao() == null ? "" : request.descricao()))
                    .retrieve()
                    .body(Map.class);
            String externalId = response == null ? null : String.valueOf(response.get("id"));
            String boleto = response == null ? null : (String) response.get("bankSlipUrl");
            return new ChargeResult(externalId, boleto, null, null, ChargeStatus.PENDENTE);
        } catch (Exception ex) {
            throw new BusinessException("Falha ao criar cobranca no Asaas: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public ChargeStatus consultarStatus(String externalId) {
        return ChargeStatus.PENDENTE;
    }

    @Override
    public void cancelarCobranca(String externalId) {
        restClient.delete().uri("/payments/{id}", externalId).retrieve().toBodilessEntity();
    }
}
