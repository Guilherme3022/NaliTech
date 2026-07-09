package com.ledgerflow.modules.movement.entity;

import com.ledgerflow.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movements")
@Getter
@Setter
@NoArgsConstructor
public class Movement extends TenantEntity {

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    private LocalDate data;

    @Column(precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(length = 300)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MovementType tipo;

    @Column(length = 60)
    private String origem;

    @Column(length = 80)
    private String documento;

    @Column(length = 80)
    private String banco;

    @Column(name = "categoria_sugerida")
    private UUID categoriaSugerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MovementStatus status = MovementStatus.NORMALIZADO;
}
