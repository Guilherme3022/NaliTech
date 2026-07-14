package com.nalitech.modules.reconciliation.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Perfil de Conciliacao (spec secao 8): configuracao reutilizavel por cliente
 * com sistema de origem, tipo de arquivo, sistema contabil de destino e plano.
 */
@Entity
@Table(name = "reconciliation_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationProfile extends TenantEntity {

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "sistema_origem", length = 100)
    private String sistemaOrigem;

    @Column(name = "tipo_arquivo", length = 100)
    private String tipoArquivo;

    @Column(name = "sistema_contabil_destino", length = 100)
    private String sistemaContabilDestino;

    @Column(name = "plano_id")
    private UUID planoId;

    @Column(nullable = false)
    private boolean ativo = true;
}
