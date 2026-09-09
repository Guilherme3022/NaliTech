package com.nalitech.modules.aiusage.service;

import com.nalitech.modules.aiusage.dto.AiUsageDtos.AiUsageResponse;
import com.nalitech.modules.aiusage.entity.AiUsageMonthly;
import com.nalitech.modules.aiusage.repository.AiUsageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra e consulta o consumo de IA por empresa. Duas saidas:
 *   - Micrometer/Prometheus (dashboard operacional cross-tenant);
 *   - agregado mensal duravel no banco (faturamento por empresa).
 */
@Slf4j
@Service
public class AiUsageService {

    public static final String FEATURE_CONCILIACAO = "CONCILIACAO";
    public static final String FEATURE_CLASSIFICACAO = "CLASSIFICACAO";

    private final AiUsageRepository repository;
    private final MeterRegistry registry;

    public AiUsageService(AiUsageRepository repository, MeterRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    /**
     * Contabiliza UMA chamada de IA. Em transacao propria (REQUIRES_NEW) para que o
     * consumo seja registrado mesmo que o processamento do item falhe depois \u2014 a
     * chamada (custo) ja aconteceu.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID empresaId, String feature, long tokensEntrada, long tokensSaida) {
        if (empresaId == null) {
            return;
        }
        try {
            registry.counter("nalitech.ai.calls",
                    "empresa", empresaId.toString(), "feature", feature).increment();
            registry.counter("nalitech.ai.tokens",
                    "empresa", empresaId.toString(), "feature", feature, "tipo", "entrada")
                    .increment(tokensEntrada);
            registry.counter("nalitech.ai.tokens",
                    "empresa", empresaId.toString(), "feature", feature, "tipo", "saida")
                    .increment(tokensSaida);

            repository.upsertIncrement(UUID.randomUUID(), empresaId,
                    YearMonth.now().toString(), feature, 1, tokensEntrada, tokensSaida);
        } catch (Exception ex) {
            // Medicao nunca deve quebrar o fluxo de negocio.
            log.warn("Falha ao registrar consumo de IA (empresa={}, feature={}): {}",
                    empresaId, feature, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AiUsageResponse> usage(UUID empresaId, String anoMes) {
        List<AiUsageMonthly> registros = (anoMes == null || anoMes.isBlank())
                ? repository.findByEmpresaId(empresaId)
                : repository.findByEmpresaIdAndAnoMes(empresaId, anoMes);
        return registros.stream()
                .map(r -> new AiUsageResponse(r.getAnoMes(), r.getFeature(), r.getChamadas(),
                        r.getTokensEntrada(), r.getTokensSaida()))
                .toList();
    }
}
