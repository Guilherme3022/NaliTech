package com.ledgerflow.modules.notification.service;

import com.ledgerflow.modules.finance.event.InvoiceEvents.InvoiceOverdueEvent;
import com.ledgerflow.modules.file.event.UploadErroEvent;
import com.ledgerflow.modules.fiscal.event.ObrigacaoVencendoEvent;
import com.ledgerflow.modules.notification.entity.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final String destinatarioEscritorio;

    public NotificationEventListener(NotificationService notificationService,
                                     @Value("${OFFICE_NOTIFICATION_EMAIL:escritorio@ledgerflow.local}")
                                     String destinatarioEscritorio) {
        this.notificationService = notificationService;
        this.destinatarioEscritorio = destinatarioEscritorio;
    }

    @Async
    @EventListener
    public void onUploadErro(UploadErroEvent event) {
        notificationService.notify(event.empresaId(), NotificationChannelType.EMAIL,
                destinatarioEscritorio,
                "Falha no processamento de upload",
                "O upload %s falhou na etapa %s: %s"
                        .formatted(event.uploadId(), event.etapa(), event.mensagem()));
    }

    @Async
    @EventListener
    public void onCobrancaVencida(InvoiceOverdueEvent event) {
        notificationService.notify(event.empresaId(), NotificationChannelType.EMAIL,
                destinatarioEscritorio,
                "Cobranca vencida",
                "A cobranca %s (valor %s) esta vencida ha %d dia(s)."
                        .formatted(event.invoiceId(), event.valor(), event.diasEmAtraso()));
    }

    @Async
    @EventListener
    public void onObrigacaoVencendo(ObrigacaoVencendoEvent event) {
        notificationService.notify(event.empresaId(), NotificationChannelType.EMAIL,
                destinatarioEscritorio,
                "Obrigacao fiscal proxima do vencimento",
                "A obrigacao '%s' vence em %s.".formatted(event.tipo(), event.vencimento()));
    }
}
