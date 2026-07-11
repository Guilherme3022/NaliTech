package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conta bancaria cadastrada: o "outro lado" do lancamento de partida dobrada.
 * Vincula um banco/caixa a uma conta do plano de contas. Uma empresa pode ter
 * varias; a(s) marcada(s) como {@code padrao} sao usadas por default.
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "conta_contabil_id", nullable = false)
    private UUID contaContabilId;

    @Column(nullable = false)
    private boolean padrao = false;

    // null = banco compartilhado (escritorio); preenchido = especifico do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
