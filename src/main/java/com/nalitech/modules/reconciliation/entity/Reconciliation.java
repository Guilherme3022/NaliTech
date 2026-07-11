package com.nalitech.modules.reconciliation.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reconciliations")
@Getter
@Setter
@NoArgsConstructor
public class Reconciliation extends TenantEntity {

    @Column(name = "movement_id", nullable = false)
    private UUID movementId;

    @Column(name = "matched_movement_id")
    private UUID matchedMovementId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatus status = ReconciliationStatus.PENDENTE;

    @Column(length = 30)
    private String camada;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(length = 250)
    private String motivo;
}
