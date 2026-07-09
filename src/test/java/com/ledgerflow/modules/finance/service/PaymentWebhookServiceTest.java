package com.ledgerflow.modules.finance.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.modules.finance.entity.PaymentWebhookEvent;
import com.ledgerflow.modules.finance.repository.OfficeInvoiceRepository;
import com.ledgerflow.modules.finance.repository.PaymentWebhookEventRepository;
import com.ledgerflow.shared.exception.BusinessException;
import com.ledgerflow.shared.util.HmacSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentWebhookEventRepository eventRepository;
    @Mock
    private OfficeInvoiceRepository invoiceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BODY =
            "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_1\"}}";

    @Test
    void eventoDuplicadoNaoReprocessa() {
        PaymentWebhookService service = new PaymentWebhookService(
                eventRepository, invoiceRepository, objectMapper, eventPublisher, "");
        when(eventRepository.existsByProviderAndExternalIdAndEventType("asaas", "pay_1", "PAYMENT_CONFIRMED"))
                .thenReturn(true);

        service.process("asaas", BODY, null);

        verify(eventRepository, never()).save(any(PaymentWebhookEvent.class));
    }

    @Test
    void assinaturaInvalidaRejeita() {
        PaymentWebhookService service = new PaymentWebhookService(
                eventRepository, invoiceRepository, objectMapper, eventPublisher, "segredo");

        assertThatThrownBy(() -> service.process("asaas", BODY, "assinatura-errada"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Assinatura");
    }

    @Test
    void assinaturaValidaEhAceita() {
        PaymentWebhookService service = new PaymentWebhookService(
                eventRepository, invoiceRepository, objectMapper, eventPublisher, "segredo");
        String signature = HmacSigner.sign(BODY, "segredo");
        when(eventRepository.existsByProviderAndExternalIdAndEventType("asaas", "pay_1", "PAYMENT_CONFIRMED"))
                .thenReturn(true);

        service.process("asaas", BODY, signature);
    }
}
