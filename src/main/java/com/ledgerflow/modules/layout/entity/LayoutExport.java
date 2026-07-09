package com.ledgerflow.modules.layout.entity;

import com.ledgerflow.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "layout_exports")
@Getter
@Setter
@NoArgsConstructor
public class LayoutExport extends TenantEntity {

    @Column(nullable = false, length = 30)
    private String sistema;

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim")
    private LocalDate periodoFim;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(nullable = false)
    private int quantidade;
}
