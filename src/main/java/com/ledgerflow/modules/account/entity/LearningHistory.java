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
@Table(name = "learning_history")
@Getter
@Setter
@NoArgsConstructor
public class LearningHistory extends TenantEntity {

    @Column(name = "descricao_padrao", nullable = false, length = 200)
    private String descricaoPadrao;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(nullable = false)
    private int ocorrencias = 1;
}
