package com.nalitech.modules.aiusage.repository;

import com.nalitech.modules.aiusage.entity.AiUsageMonthly;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageRepository extends JpaRepository<AiUsageMonthly, UUID> {

    /**
     * Incremento atomico do agregado mensal (cria se nao existir). O :id so e usado
     * quando a linha e nova; no conflito, apenas soma os contadores.
     */
    @Modifying
    @Query(value = """
            insert into ai_usage_monthly
                (id, empresa_id, ano_mes, feature, chamadas, tokens_entrada, tokens_saida, atualizado_em)
            values
                (:id, :empresaId, :anoMes, :feature, :chamadas, :tokensEntrada, :tokensSaida, now())
            on conflict (empresa_id, ano_mes, feature) do update set
                chamadas       = ai_usage_monthly.chamadas + excluded.chamadas,
                tokens_entrada = ai_usage_monthly.tokens_entrada + excluded.tokens_entrada,
                tokens_saida   = ai_usage_monthly.tokens_saida + excluded.tokens_saida,
                atualizado_em  = now()
            """, nativeQuery = true)
    void upsertIncrement(@Param("id") UUID id,
                         @Param("empresaId") UUID empresaId,
                         @Param("anoMes") String anoMes,
                         @Param("feature") String feature,
                         @Param("chamadas") long chamadas,
                         @Param("tokensEntrada") long tokensEntrada,
                         @Param("tokensSaida") long tokensSaida);

    List<AiUsageMonthly> findByEmpresaIdAndAnoMes(UUID empresaId, String anoMes);

    List<AiUsageMonthly> findByEmpresaId(UUID empresaId);
}
