package com.nalitech.modules.notification.channel;

import com.nalitech.modules.notification.entity.Notification;
import com.nalitech.modules.notification.entity.NotificationChannelType;

public interface NotificationChannel {

    boolean supports(NotificationChannelType type);

    void send(Notification notification);
}
