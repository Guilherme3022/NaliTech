package com.nalitech.modules.finance.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "office_fees")
@Getter
@Setter
@NoArgsConstructor
public class OfficeFee extends TenantEntity {

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(length = 150)
    private String descricao;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(length = 20)
    private String periodicidade;

    @Column(nullable = false)
    private boolean ativo = true;
}
