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

/** Contrato de emprestimo/financiamento e suas contas contabeis. */
@Entity
@Table(name = "loan_contracts")
@Getter
@Setter
@NoArgsConstructor
public class LoanContract extends TenantEntity {

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(name = "valor_total", precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "taxa_juros", precision = 9, scale = 4)
    private BigDecimal taxaJuros;

    private Integer parcelas;

    @Column(name = "conta_principal_id")
    private UUID contaPrincipalId;

    @Column(name = "conta_juros_id")
    private UUID contaJurosId;

    @Column(name = "conta_encargos_id")
    private UUID contaEncargosId;

    // CURTO | LONGO (classificacao entre curto e longo prazo).
    @Column(name = "classificacao_prazo", length = 10)
    private String classificacaoPrazo;

    @Column(nullable = false)
    private boolean ativo = true;

    // null = compartilhado (escritorio); preenchido = especifico do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
