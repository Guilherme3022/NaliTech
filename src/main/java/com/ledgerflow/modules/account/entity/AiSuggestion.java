package com.ledgerflow.modules.account.entity;

import com.ledgerflow.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_suggestions")
@Getter
@Setter
@NoArgsConstructor
public class AiSuggestion extends TenantEntity {

    @Column(name = "movement_id", nullable = false)
    private UUID movementId;

    @Column(name = "conta_sugerida")
    private UUID contaSugerida;

    @Column(precision = 5, scale = 2)
    private BigDecimal confianca;

    @Column(length = 20)
    private String origem;
}
