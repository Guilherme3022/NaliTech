package com.ledgerflow.modules.notification.service;

import com.ledgerflow.modules.notification.channel.NotificationChannel;
import com.ledgerflow.modules.notification.entity.Notification;
import com.ledgerflow.modules.notification.entity.NotificationChannelType;
import com.ledgerflow.modules.notification.entity.NotificationStatus;
import com.ledgerflow.modules.notification.repository.NotificationRepository;
import com.ledgerflow.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class NotificationService {

    private final List<NotificationChannel> channels;
    private final NotificationRepository repository;

    public NotificationService(List<NotificationChannel> channels, NotificationRepository repository) {
        this.channels = channels;
        this.repository = repository;
    }

    public Notification notify(UUID empresaId, NotificationChannelType canal, String destinatario,
                               String assunto, String corpo) {
        Notification notification = new Notification();
        notification.setEmpresaId(empresaId);
        notification.setCanal(canal);
        notification.setDestinatario(destinatario);
        notification.setAssunto(assunto);
        notification.setCorpo(corpo);
        notification = repository.save(notification);

        try {
            resolveChannel(canal).send(notification);
            notification.setStatus(NotificationStatus.ENVIADA);
            notification.setEnviadoEm(OffsetDateTime.now());
        } catch (Exception ex) {
            log.warn("Falha ao enviar notificacao {}: {}", notification.getId(), ex.getMessage());
            notification.setStatus(NotificationStatus.FALHA);
            notification.setErro(ex.getMessage());
        }
        return repository.save(notification);
    }

    private NotificationChannel resolveChannel(NotificationChannelType canal) {
        return channels.stream()
                .filter(channel -> channel.supports(canal))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Canal de notificacao nao suportado: " + canal, HttpStatus.BAD_REQUEST));
    }
}
