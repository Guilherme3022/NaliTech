package com.nalitech.modules.movement.entity;

import com.nalitech.shared.domain.TenantEntity;
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

    // Cliente (CNPJ) dono deste lancamento, propagado do upload (Increment 3).
    @Column(name = "cliente_id")
    private UUID clienteId;

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

    // Lancamento de partida dobrada (Increment 2).
    @Column(name = "conta_debito_id")
    private UUID contaDebitoId;

    @Column(name = "conta_credito_id")
    private UUID contaCreditoId;

    // Centro de custo (Increment 4).
    @Column(name = "centro_custo_id")
    private UUID centroCustoId;

    // Filial (Increment 5).
    @Column(name = "filial_id")
    private UUID filialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MovementStatus status = MovementStatus.NORMALIZADO;
}
