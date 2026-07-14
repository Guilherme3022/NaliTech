package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Conta de um plano-modelo (spec secao 3/4). */
@Entity
@Table(name = "plano_modelo_contas")
@Getter
@Setter
@NoArgsConstructor
public class PlanoModeloConta extends TenantEntity {

    @Column(name = "modelo_id", nullable = false)
    private UUID modeloId;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 20)
    private String tipo;
}
