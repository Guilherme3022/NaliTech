package com.nalitech.modules.account.ai;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Resolve a ordem dos provedores de sugestao de conta (todos deterministicos e de custo
 * zero — a IA externa foi removida do produto). O primeiro provedor que sugerir vence.
 *
 * <p>Ordem: histórico de aprendizado do cliente ({@link HeuristicSuggestionProvider}) e,
 * em seguida, casamento por nome contra o plano de contas do cliente
 * ({@link ChartNameSuggestionProvider}).</p>
 */
@Component
public class SuggestionProviderSelector {

    private final HeuristicSuggestionProvider heuristic;
    private final ChartNameSuggestionProvider chartName;

    public SuggestionProviderSelector(HeuristicSuggestionProvider heuristic,
                                      ChartNameSuggestionProvider chartName) {
        this.heuristic = heuristic;
        this.chartName = chartName;
    }

    /** Provedores a tentar, em ordem (o primeiro que sugerir vence). */
    public List<AiSuggestionProvider> providers() {
        return List.of(heuristic, chartName);
    }

    /**
     * Provedores deterministicos e de custo zero. Como nao ha mais IA externa, e o mesmo
     * conjunto de {@link #providers()} — mantido para compatibilidade das chamadas em lote
     * do pipeline de conciliacao.
     */
    public List<AiSuggestionProvider> deterministicProviders() {
        return providers();
    }
}
