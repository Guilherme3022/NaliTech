package com.ledgerflow.modules.webhook.service;

import com.ledgerflow.modules.webhook.dto.WebhookDtos.CreateSubscriptionRequest;
import com.ledgerflow.modules.webhook.dto.WebhookDtos.SubscriptionResponse;
import com.ledgerflow.modules.webhook.entity.WebhookDelivery;
import com.ledgerflow.modules.webhook.entity.WebhookSubscription;
import com.ledgerflow.modules.webhook.repository.WebhookDeliveryRepository;
import com.ledgerflow.modules.webhook.repository.WebhookSubscriptionRepository;
import com.ledgerflow.security.SecurityUtils;
import com.ledgerflow.shared.exception.BusinessException;
import com.ledgerflow.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcherService dispatcher;

    public WebhookSubscriptionService(WebhookSubscriptionRepository subscriptionRepository,
                                      WebhookDeliveryRepository deliveryRepository,
                                      WebhookDispatcherService dispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.dispatcher = dispatcher;
    }

    public SubscriptionResponse create(CreateSubscriptionRequest request) {
        validateUrl(request.urlDestino());
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setEmpresaId(SecurityUtils.currentEmpresaId());
        subscription.setEvento(request.evento());
        subscription.setUrlDestino(request.urlDestino());
        subscription.setSegredo(UUID.randomUUID().toString().replace("-", ""));
        WebhookSubscription saved = subscriptionRepository.save(subscription);

        return new SubscriptionResponse(saved.getId(), saved.getEvento(), saved.getUrlDestino(),
                saved.isAtivo(), saved.getSegredo());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list() {
        return subscriptionRepository.findByEmpresaId(SecurityUtils.currentEmpresaId()).stream()
                .map(s -> new SubscriptionResponse(s.getId(), s.getEvento(), s.getUrlDestino(),
                        s.isAtivo(), null))
                .toList();
    }

    public void delete(UUID id) {
        subscriptionRepository.delete(findInCurrentCompany(id));
    }

    @Transactional(readOnly = true)
    public Page<WebhookDelivery> deliveries(UUID subscriptionId, Pageable pageable) {
        return deliveryRepository.findBySubscriptionIdAndEmpresaId(
                subscriptionId, SecurityUtils.currentEmpresaId(), pageable);
    }

    public void sendTest(UUID id) {
        WebhookSubscription subscription = findInCurrentCompany(id);
        dispatcher.dispatch(subscription.getEmpresaId(), subscription.getEvento(),
                Map.of("teste", true, "mensagem", "Evento de teste do LedgerFlow"));
    }

    private WebhookSubscription findInCurrentCompany(UUID id) {
        return subscriptionRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura nao encontrada."));
    }

    private void validateUrl(String url) {
        String normalized = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new BusinessException(
                    "URL de webhook invalida: use http ou https.", HttpStatus.BAD_REQUEST);
        }
    }
}
