package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Filial de um cliente (matriz/filiais), com CNPJ proprio. */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch extends TenantEntity {

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 14)
    private String cnpj;

    @Column(nullable = false)
    private boolean ativo = true;

    // null = compartilhada (escritorio); preenchido = especifica do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
