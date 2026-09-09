package com.nalitech.modules.aiusage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agregado mensal de consumo de IA por empresa e feature. Alimentado por UPSERT
 * atomico (ver {@code AiUsageRepository}); usado para faturar o custo real da IA
 * por empresa. Nao estende TenantEntity de proposito: o incremento e feito via
 * query nativa, sem passar pela auditoria/JPA.
 */
@Entity
@Table(name = "ai_usage_monthly")
@Getter
@Setter
@NoArgsConstructor
public class AiUsageMonthly {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "ano_mes", nullable = false, length = 7)
    private String anoMes;

    @Column(nullable = false, length = 40)
    private String feature;

    @Column(nullable = false)
    private long chamadas;

    @Column(name = "tokens_entrada", nullable = false)
    private long tokensEntrada;

    @Column(name = "tokens_saida", nullable = false)
    private long tokensSaida;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;
}
