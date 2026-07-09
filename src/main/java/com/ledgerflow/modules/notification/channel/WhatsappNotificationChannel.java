package com.ledgerflow.modules.notification.channel;

import com.ledgerflow.modules.notification.entity.Notification;
import com.ledgerflow.modules.notification.entity.NotificationChannelType;
import org.springframework.stereotype.Component;

@Component
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
