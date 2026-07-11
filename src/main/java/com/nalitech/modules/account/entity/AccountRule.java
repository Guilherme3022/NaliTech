package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_rules")
@Getter
@Setter
@NoArgsConstructor
public class AccountRule extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao_contains", length = 200)
    private String descricaoContains;

    @Column(name = "valor_operador", length = 5)
    private String valorOperador;

    @Column(name = "valor_ref", precision = 18, scale = 2)
    private BigDecimal valorRef;

    @Column(name = "conta_id")
    private UUID contaId;

    @Column(name = "marcar_revisao", nullable = false)
    private boolean marcarRevisao = false;

    @Column(nullable = false)
    private int prioridade = 0;

    @Column(nullable = false)
    private boolean ativo = true;
}
