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
@Table(name = "learning_history")
@Getter
@Setter
@NoArgsConstructor
public class LearningHistory extends TenantEntity {

    @Column(name = "descricao_padrao", nullable = false, length = 200)
    private String descricaoPadrao;

    // Partida dobrada aprendida: conta de debito e de credito escolhidas pelo contador.
    @Column(name = "conta_debito_id")
    private UUID contaDebitoId;

    @Column(name = "conta_credito_id")
    private UUID contaCreditoId;

    @Column(nullable = false)
    private int ocorrencias = 1;

    // Aprendizado por cliente (Increment 3). null = escopo do escritorio.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
