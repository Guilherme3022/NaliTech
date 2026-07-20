package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chart_of_accounts")
@Getter
@Setter
@NoArgsConstructor
public class ChartOfAccount extends TenantEntity {

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 20)
    private String tipo;

    // true = analitica (lancavel); false = sintetica (agrupadora); null = indefinida.
    @Column(name = "analitica")
    private Boolean analitica;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "parent_id")
    private UUID parentId;

    // null = conta compartilhada (escritorio); preenchido = especifica do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
