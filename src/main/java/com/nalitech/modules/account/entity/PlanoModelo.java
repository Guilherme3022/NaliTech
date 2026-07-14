package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plano-modelo reutilizavel, por empresa (spec secao 3). Serve de base para
 * copiar a estrutura de contas ao plano de contas de um cliente.
 */
@Entity
@Table(name = "plano_modelos")
@Getter
@Setter
@NoArgsConstructor
public class PlanoModelo extends TenantEntity {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 300)
    private String descricao;
}
