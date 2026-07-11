package com.nalitech.modules.webhook.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class WebhookSubscription extends TenantEntity {

    @Column(nullable = false, length = 40)
    private String evento;

    @Column(name = "url_destino", nullable = false, length = 500)
    private String urlDestino;

    @Column(nullable = false, length = 120)
    private String segredo;

    @Column(nullable = false)
    private boolean ativo = true;
}
