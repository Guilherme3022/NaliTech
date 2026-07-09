package com.ledgerflow.modules.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.modules.webhook.entity.WebhookDelivery;
import com.ledgerflow.modules.webhook.entity.WebhookSubscription;
import com.ledgerflow.modules.webhook.repository.WebhookDeliveryRepository;
import com.ledgerflow.modules.webhook.repository.WebhookSubscriptionRepository;
import com.ledgerflow.shared.util.HmacSigner;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WebhookDispatcherService {

    private static final int MAX_TENTATIVAS = 3;
    private static final String SIGNATURE_HEADER = "X-LedgerFlow-Signature";

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public WebhookDispatcherService(WebhookSubscriptionRepository subscriptionRepository,
                                    WebhookDeliveryRepository deliveryRepository,
                                    ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void dispatch(UUID empresaId, String evento, Map<String, Object> data) {
        List<WebhookSubscription> subscriptions =
                subscriptionRepository.findByEmpresaIdAndEventoAndAtivoTrue(empresaId, evento);
        if (subscriptions.isEmpty()) {
            return;
        }
        String payload = buildPayload(empresaId, evento, data);
        subscriptions.forEach(subscription -> deliver(subscription, evento, payload));
    }

    @Transactional
    void deliver(WebhookSubscription subscription, String evento, String payload) {
        String signature = HmacSigner.sign(payload, subscription.getSegredo());
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                HttpResponse<String> response = send(subscription.getUrlDestino(), payload, signature);
                boolean sucesso = response.statusCode() >= 200 && response.statusCode() < 300;
                registrar(subscription, evento, payload, tentativa, response.statusCode(), sucesso, null);
                if (sucesso) {
                    return;
                }
            } catch (Exception ex) {
                registrar(subscription, evento, payload, tentativa, null, false, ex.getMessage());
            }
            backoff(tentativa);
        }
    }

    private HttpResponse<String> send(String url, String payload, String signature) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header(SIGNATURE_HEADER, signature)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void registrar(WebhookSubscription subscription, String evento, String payload,
                           int tentativa, Integer status, boolean sucesso, String erro) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setEmpresaId(subscription.getEmpresaId());
        delivery.setSubscriptionId(subscription.getId());
        delivery.setEvento(evento);
        delivery.setPayload(payload);
        delivery.setHttpStatus(status);
        delivery.setTentativa(tentativa);
        delivery.setSucesso(sucesso);
        delivery.setErro(erro);
        deliveryRepository.save(delivery);
    }

    private String buildPayload(UUID empresaId, String evento, Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "event", evento,
                    "empresa_id", empresaId.toString(),
                    "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    "data", data));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload de webhook.", ex);
        }
    }

    private void backoff(int tentativa) {
        try {

            Thread.sleep(Duration.ofSeconds((long) Math.pow(2, tentativa - 1)).toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
