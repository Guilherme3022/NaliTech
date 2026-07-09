package com.ledgerflow.modules.finance.entity;

import com.ledgerflow.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_webhook_events")
@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookEvent extends AuditableEntity {

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private boolean processado = false;
}
