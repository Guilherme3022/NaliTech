package com.nalitech.modules.account.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolve a ordem dos provedores de sugestao conforme a configuracao.
 *   AI_PROVIDER=HEURISTICA (padrao) -> so heuristica (custo zero)
 *   AI_PROVIDER=IA                  -> tenta o LLM e cai na heuristica se falhar
 */
@Component
public class SuggestionProviderSelector {

    private final HeuristicSuggestionProvider heuristic;
    private final LlmSuggestionProvider llm;
    private final String provider;

    public SuggestionProviderSelector(HeuristicSuggestionProvider heuristic,
                                      LlmSuggestionProvider llm,
                                      @Value("${AI_PROVIDER:HEURISTICA}") String provider) {
        this.heuristic = heuristic;
        this.llm = llm;
        this.provider = provider;
    }

    /** Provedores a tentar, em ordem (o primeiro que sugerir vence). */
    public List<AiSuggestionProvider> providers() {
        if ("IA".equalsIgnoreCase(provider) && llm.isConfigured()) {
            return List.of(llm, heuristic);
        }
        return List.of(heuristic);
    }

    /**
     * Apenas os provedores deterministicos e de custo zero (heuristica/aprendizado),
     * sem chamar LLM. Usado na sugestao proativa em lote do pipeline de conciliacao,
     * onde acionar a IA por movimentacao seria caro/lento.
     */
    public List<AiSuggestionProvider> deterministicProviders() {
        return List.of(heuristic);
    }
}
