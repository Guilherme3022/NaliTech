package com.ledgerflow.modules.reconciliation.entity;

import com.ledgerflow.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reconciliation_rules")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationRule extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao_contains", length = 200)
    private String descricaoContains;

    @Column(name = "valor_min", precision = 18, scale = 2)
    private BigDecimal valorMin;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(nullable = false)
    private boolean ativo = true;
}
