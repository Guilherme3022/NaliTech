package com.ledgerflow.modules.notification.channel;

import com.ledgerflow.modules.notification.entity.Notification;
import com.ledgerflow.modules.notification.entity.NotificationChannelType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Canal WhatsApp OCULTO por padrao (nao implementado nativamente; ver E20/n8n).
// Para reativar, defina no ambiente: notifications.whatsapp.enabled=true
@Component
@ConditionalOnProperty(name = "notifications.whatsapp.enabled", havingValue = "true")
public class WhatsappNotificationChannel implements NotificationChannel {

    @Override
    public boolean supports(NotificationChannelType type) {
        return type == NotificationChannelType.WHATSAPP;
    }

    @Override
    public void send(Notification notification) {
        throw new UnsupportedOperationException(
                "Canal WhatsApp nao implementado nativamente: usar o fluxo do n8n (E20).");
    }
}
