package com.nalitech.modules.account.service;

import com.nalitech.modules.account.ai.AiSuggestionProvider;
import com.nalitech.modules.account.ai.AiSuggestionProvider.SuggestedAccount;
import com.nalitech.modules.account.ai.SuggestionProviderSelector;
import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.entity.AiSuggestion;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.AiSuggestionRepository;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sugere a conta contabil de uma movimentacao: primeiro por regra explicita
 * (deterministica), depois pelo provedor de sugestao ativo (heuristica ou IA).
 */
@Service
@Transactional
public class ClassificationSuggestionService {

    private final RuleEngineService ruleEngineService;
    private final AiSuggestionRepository suggestionRepository;
    private final MovementRepository movementRepository;
    private final ChartOfAccountRepository chartRepository;
    private final SuggestionProviderSelector providerSelector;

    public ClassificationSuggestionService(RuleEngineService ruleEngineService,
                                           AiSuggestionRepository suggestionRepository,
                                           MovementRepository movementRepository,
                                           ChartOfAccountRepository chartRepository,
                                           SuggestionProviderSelector providerSelector) {
        this.ruleEngineService = ruleEngineService;
        this.suggestionRepository = suggestionRepository;
        this.movementRepository = movementRepository;
        this.chartRepository = chartRepository;
        this.providerSelector = providerSelector;
    }

    public AiSuggestion suggestFor(UUID movementId) {
        Movement movement = movementRepository
                .findByIdAndEmpresaId(movementId, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));

        Optional<AccountRule> rule = ruleEngineService.firstMatching(movement);
        if (rule.isPresent() && rule.get().getContaId() != null) {
            return persist(movement, rule.get().getContaId(), BigDecimal.valueOf(95), "REGRA");
        }

        List<ChartOfAccount> contas = chartRepository.findByEmpresaId(movement.getEmpresaId());
        for (AiSuggestionProvider provider : providerSelector.providers()) {
            Optional<SuggestedAccount> sugestao = provider.suggest(movement, contas);
            if (sugestao.isPresent()) {
                return persist(movement, sugestao.get().contaId(), sugestao.get().confianca(),
                        provider.origem());
            }
        }

        return persist(movement, null, BigDecimal.ZERO, "NENHUMA");
    }

    private AiSuggestion persist(Movement movement, UUID contaId, BigDecimal confianca, String origem) {
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setEmpresaId(movement.getEmpresaId());
        suggestion.setMovementId(movement.getId());
        suggestion.setContaSugerida(contaId);
        suggestion.setConfianca(confianca);
        suggestion.setOrigem(origem);
        return suggestionRepository.save(suggestion);
    }
}
