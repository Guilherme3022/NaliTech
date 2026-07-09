package com.ledgerflow.modules.account.entity;

import com.ledgerflow.shared.domain.TenantEntity;
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

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "parent_id")
    private UUID parentId;
}
