package com.ledgerflow.modules.notification.channel;

import com.ledgerflow.modules.notification.entity.Notification;
import com.ledgerflow.modules.notification.entity.NotificationChannelType;

public interface NotificationChannel {

    boolean supports(NotificationChannelType type);

    void send(Notification notification);
}
