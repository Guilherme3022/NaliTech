package com.nalitech.modules.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nalitech.modules.finance.entity.InvoiceStatus;
import com.nalitech.modules.finance.entity.OfficeInvoice;
import com.nalitech.modules.finance.entity.PaymentWebhookEvent;
import com.nalitech.modules.finance.event.InvoiceEvents.InvoiceOverdueEvent;
import com.nalitech.modules.finance.event.InvoiceEvents.InvoicePaidEvent;
import com.nalitech.modules.finance.repository.OfficeInvoiceRepository;
import com.nalitech.modules.finance.repository.PaymentWebhookEventRepository;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.util.HmacSigner;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class PaymentWebhookService {

    private final PaymentWebhookEventRepository eventRepository;
    private final OfficeInvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final String webhookSecret;

    public PaymentWebhookService(PaymentWebhookEventRepository eventRepository,
                                 OfficeInvoiceRepository invoiceRepository,
                                 ObjectMapper objectMapper,
                                 org.springframework.context.ApplicationEventPublisher eventPublisher,
                                 @Value("${PAYMENT_WEBHOOK_SECRET:}") String webhookSecret) {
        this.eventRepository = eventRepository;
        this.invoiceRepository = invoiceRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void process(String provider, String rawBody, String signature) {
        validateSignature(rawBody, signature);

        JsonNode root = parse(rawBody);
        String eventType = text(root, "event");
        String externalId = extractExternalId(root);
        if (externalId == null || eventType == null) {
            throw new BusinessException("Payload de webhook invalido.", HttpStatus.BAD_REQUEST);
        }

        if (eventRepository.existsByProviderAndExternalIdAndEventType(provider, externalId, eventType)) {
            log.info("Evento de webhook duplicado ignorado: {} {}", provider, externalId);
            return;
        }

        OfficeInvoice invoice = invoiceRepository.findByProviderAndExternalId(provider, externalId)
                .orElse(null);
        persistEvent(provider, externalId, eventType, rawBody, invoice);

        if (invoice != null) {
            applyStatus(invoice, eventType);
        }
    }

    private void applyStatus(OfficeInvoice invoice, String eventType) {
        String normalized = eventType.toUpperCase();
        if (normalized.contains("CONFIRM") || normalized.contains("RECEIVED") || normalized.contains("PAGO")) {
            invoice.setStatus(InvoiceStatus.PAGO);
            invoice.setPagoEm(OffsetDateTime.now());
            invoiceRepository.save(invoice);
            eventPublisher.publishEvent(new InvoicePaidEvent(
                    invoice.getId(), invoice.getEmpresaId(), invoice.getClienteId(), invoice.getValor()));
        } else if (normalized.contains("OVERDUE") || normalized.contains("VENCID")) {
            invoice.setStatus(InvoiceStatus.VENCIDO);
            invoiceRepository.save(invoice);
            long dias = java.time.temporal.ChronoUnit.DAYS.between(
                    invoice.getVencimento(), java.time.LocalDate.now());
            eventPublisher.publishEvent(new InvoiceOverdueEvent(
                    invoice.getId(), invoice.getEmpresaId(), invoice.getClienteId(),
                    invoice.getValor(), Math.max(dias, 0)));
        }
    }

    private void persistEvent(String provider, String externalId, String eventType,
                              String rawBody, OfficeInvoice invoice) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(provider);
        event.setExternalId(externalId);
        event.setEventType(eventType);
        event.setPayload(rawBody);
        event.setProcessado(invoice != null);
        event.setEmpresaId(invoice != null ? invoice.getEmpresaId() : null);
        eventRepository.save(event);
    }

    private void validateSignature(String rawBody, String signature) {
        if (!StringUtils.hasText(webhookSecret)) {

            return;
        }
        if (!HmacSigner.matches(rawBody, webhookSecret, signature)) {
            throw new BusinessException("Assinatura de webhook invalida.", HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractExternalId(JsonNode root) {
        if (root.hasNonNull("payment") && root.get("payment").hasNonNull("id")) {
            return root.get("payment").get("id").asText();
        }
        return text(root, "id");
    }

    private JsonNode parse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BusinessException("JSON de webhook invalido.", HttpStatus.BAD_REQUEST);
        }
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
