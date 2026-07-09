package com.ledgerflow.modules.notification.channel;

import com.ledgerflow.modules.notification.entity.Notification;
import com.ledgerflow.modules.notification.entity.NotificationChannelType;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationChannel implements NotificationChannel {

    @Override
    public boolean supports(NotificationChannelType type) {
        return type == NotificationChannelType.PUSH;
    }

    @Override
    public void send(Notification notification) {
        throw new UnsupportedOperationException("Canal push nao implementado ainda.");
    }
}
