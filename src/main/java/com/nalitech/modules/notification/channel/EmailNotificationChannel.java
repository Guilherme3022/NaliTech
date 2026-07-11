package com.nalitech.modules.notification.channel;

import com.nalitech.modules.notification.entity.Notification;
import com.nalitech.modules.notification.entity.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailNotificationChannel(JavaMailSender mailSender,
                                    @Value("${MAIL_FROM:no-reply@nalitech.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean supports(NotificationChannelType type) {
        return type == NotificationChannelType.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.getDestinatario());
        message.setSubject(notification.getAssunto());
        message.setText(notification.getCorpo());
        mailSender.send(message);
        log.info("E-mail enviado para {}", notification.getDestinatario());
    }
}
