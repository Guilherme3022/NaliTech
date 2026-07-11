package com.nalitech.modules.layout.exporter;

import java.util.Map;
import java.util.UUID;

/**
 * Contexto de exportacao: resolve o codigo do plano de contas, do centro de
 * custo e da filial a partir do id (o arquivo contabil usa o codigo, nao o UUID).
 */
public final class ExportContext {

    private final Map<UUID, String> codigosPorConta;
    private final Map<UUID, String> codigosPorCentroCusto;
    private final Map<UUID, String> codigosPorFilial;

    public ExportContext(Map<UUID, String> codigosPorConta,
                         Map<UUID, String> codigosPorCentroCusto,
                         Map<UUID, String> codigosPorFilial) {
        this.codigosPorConta = codigosPorConta;
        this.codigosPorCentroCusto = codigosPorCentroCusto;
        this.codigosPorFilial = codigosPorFilial;
    }

    /** Codigo da conta ou string vazia se nao houver conta/codigo. */
    public String codigo(UUID contaId) {
        return contaId == null ? "" : codigosPorConta.getOrDefault(contaId, "");
    }

    /** Codigo do centro de custo ou string vazia. */
    public String centroCusto(UUID centroCustoId) {
        return centroCustoId == null ? "" : codigosPorCentroCusto.getOrDefault(centroCustoId, "");
    }

    /** Codigo da filial ou string vazia. */
    public String filial(UUID filialId) {
        return filialId == null ? "" : codigosPorFilial.getOrDefault(filialId, "");
    }
}
