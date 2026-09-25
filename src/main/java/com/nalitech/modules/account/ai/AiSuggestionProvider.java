package com.nalitech.modules.account.ai;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.movement.entity.Movement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provedor de sugestao de conta contabil para uma movimentacao. Implementacoes:
 * heuristica (aprendizado por historico) e IA (LLM). O selecionado e resolvido
 * por configuracao, com fallback para a heuristica.
 */
public interface AiSuggestionProvider {

    /** Nome/origem da sugestao (ex.: HISTORICO, IA). */
    String origem();

    /** Sugere uma conta para a movimentacao, dado o plano de contas disponivel. */
    Optional<SuggestedAccount> suggest(Movement movement, List<ChartOfAccount> contas);

    /**
     * Sugestao de partida dobrada: conta de debito e/ou credito + confianca (0-100).
     * Um dos lados pode ser {@code null} quando o provedor so conhece a contrapartida
     * (o lado do banco e preenchido depois pelo {@code DoubleEntryService}). Quando os
     * dois lados vem preenchidos (memoria aprendida), o par e usado como esta.
     */
    record SuggestedAccount(UUID contaDebitoId, UUID contaCreditoId, BigDecimal confianca) {
        /** Atalho para uma unica contrapartida (lado a ser resolvido depois). */
        public static SuggestedAccount contrapartida(UUID contaId, BigDecimal confianca) {
            return new SuggestedAccount(contaId, null, confianca);
        }
    }
}
