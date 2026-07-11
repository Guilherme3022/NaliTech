package com.nalitech.modules.fiscal.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fiscal_obligations")
@Getter
@Setter
@NoArgsConstructor
public class FiscalObligation extends TenantEntity {

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(length = 200)
    private String descricao;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObligationStatus status = ObligationStatus.PENDENTE;
}
