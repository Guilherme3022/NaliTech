package com.nalitech.modules.aiusage.dto;

public final class AiUsageDtos {

    private AiUsageDtos() {
    }

    /** Consumo de IA agregado por competencia (YYYY-MM) e feature. */
    public record AiUsageResponse(String anoMes, String feature, long chamadas,
                                  long tokensEntrada, long tokensSaida) {
    }
}
