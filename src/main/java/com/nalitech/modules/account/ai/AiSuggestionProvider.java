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

    /** Conta sugerida + confianca (0-100). */
    record SuggestedAccount(UUID contaId, BigDecimal confianca) {
    }
}
