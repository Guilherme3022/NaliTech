package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Centro de custo para apropriacao de lancamentos (ex.: Comercial, Tecnologia). */
@Entity
@Table(name = "cost_centers")
@Getter
@Setter
@NoArgsConstructor
public class CostCenter extends TenantEntity {

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    // null = compartilhado (escritorio); preenchido = especifico do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
