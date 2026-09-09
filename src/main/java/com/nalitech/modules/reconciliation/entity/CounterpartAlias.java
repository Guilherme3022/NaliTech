package com.nalitech.modules.reconciliation.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apelido de contraparte aprendido de um match confirmado: liga o nome (normalizado)
 * que aparece no extrato ao nome que aparece no sistema, quando o contador confirma
 * que sao a mesma parte. Alimenta o match automatico futuro.
 */
@Entity
@Table(name = "counterpart_aliases")
@Getter
@Setter
@NoArgsConstructor
public class CounterpartAlias extends TenantEntity {

    @Column(name = "cliente_id")
    private UUID clienteId;

    // Nomes normalizados em ordem canonica (nomeA <= nomeB).
    @Column(name = "nome_a", nullable = false, length = 200)
    private String nomeA;

    @Column(name = "nome_b", nullable = false, length = 200)
    private String nomeB;

    @Column(nullable = false)
    private int ocorrencias = 1;
}
