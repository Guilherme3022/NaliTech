package com.nalitech.modules.reconciliation.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma "perna" de um pareamento N:1: liga uma conciliacao (cujo movimento principal e o
 * lancamento do extrato) a uma das varias movimentacoes do sistema que, somadas, batem
 * com o valor do extrato. Ex.: um deposito unico que quita varias duplicatas.
 */
@Entity
@Table(name = "reconciliation_matches")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationMatch extends TenantEntity {

    @Column(name = "reconciliation_id", nullable = false)
    private UUID reconciliationId;

    @Column(name = "movement_id", nullable = false)
    private UUID movementId;

    @Column(precision = 18, scale = 2)
    private BigDecimal valor;
}
