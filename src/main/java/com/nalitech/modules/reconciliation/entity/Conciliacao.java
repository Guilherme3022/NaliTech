package com.nalitech.modules.reconciliation.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conciliacao como lote/processo mensal de um cliente (spec secoes 9-12).
 * Agrupa os itens de {@link Reconciliation} por cliente + competencia + perfil.
 */
@Entity
@Table(name = "conciliacoes")
@Getter
@Setter
@NoArgsConstructor
public class Conciliacao extends TenantEntity {

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private LocalDate competencia;

    // Perfil de Conciliacao (EC) — vinculado em fase posterior.
    @Column(name = "perfil_id")
    private UUID perfilId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConciliacaoSituacao situacao = ConciliacaoSituacao.RASCUNHO;
}
