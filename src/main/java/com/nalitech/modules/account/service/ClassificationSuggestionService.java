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
    private final DoubleEntryService doubleEntryService;

    public ClassificationSuggestionService(RuleEngineService ruleEngineService,
                                           AiSuggestionRepository suggestionRepository,
                                           MovementRepository movementRepository,
                                           ChartOfAccountRepository chartRepository,
                                           SuggestionProviderSelector providerSelector,
                                           DoubleEntryService doubleEntryService) {
        this.ruleEngineService = ruleEngineService;
        this.suggestionRepository = suggestionRepository;
        this.movementRepository = movementRepository;
        this.chartRepository = chartRepository;
        this.providerSelector = providerSelector;
        this.doubleEntryService = doubleEntryService;
    }

    public AiSuggestion suggestFor(UUID movementId) {
        Movement movement = movementRepository
                .findByIdAndEmpresaId(movementId, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));
        return suggest(movement);
    }

    /**
     * Gera e persiste a sugestao de conta para uma movimentacao ja carregada. Nao usa
     * contexto de seguranca (usa {@code movement.getEmpresaId()}), podendo ser chamado
     * pelo pipeline assincrono de conciliacao para ja deixar a conta pre-sugerida no
     * item — antes mesmo do contador abrir a tela.
     */
    public AiSuggestion suggest(Movement movement) {
        return suggest(movement, true);
    }

    /**
     * Sugestao proativa de custo zero (regra + aprendizado, sem LLM), usada em lote pelo
     * pipeline de conciliacao para ja deixar a conta pre-sugerida no item.
     */
    public AiSuggestion suggestDeterministic(Movement movement) {
        return suggest(movement, false);
    }

    private AiSuggestion suggest(Movement movement, boolean incluirIa) {
        SuggestedAccount par = null;
        String origem = "NENHUMA";
        BigDecimal confianca = BigDecimal.ZERO;

        Optional<AccountRule> rule = ruleEngineService.firstMatching(movement);
        if (rule.isPresent() && rule.get().getContaId() != null) {
            // A conta da regra e a contrapartida; o lado (debito/credito) sai da direcao.
            par = SuggestedAccount.contrapartida(rule.get().getContaId(), BigDecimal.valueOf(95));
            origem = "REGRA";
            confianca = BigDecimal.valueOf(95);
        } else {
            // Sugestoes so podem apontar para contas lancaveis (analiticas) DO PROPRIO CLIENTE
            // (mais as compartilhadas do escritorio, cliente_id nulo). Nunca contas de outro
            // cliente da mesma empresa — senao os planos se misturam.
            List<ChartOfAccount> contas = chartRepository.findLancaveisForCliente(
                    movement.getEmpresaId(), movement.getClienteId());
            List<AiSuggestionProvider> provedores = incluirIa
                    ? providerSelector.providers()
                    : providerSelector.deterministicProviders();
            for (AiSuggestionProvider provider : provedores) {
                Optional<SuggestedAccount> sugestao = provider.suggest(movement, contas);
                if (sugestao.isPresent()) {
                    par = sugestao.get();
                    origem = provider.origem();
                    confianca = sugestao.get().confianca();
                    break;
                }
            }
        }

        if (par == null) {
            return persist(movement, null, BigDecimal.ZERO, "NENHUMA");
        }

        // Completa a partida dobrada: o lado que faltar recebe a conta do banco.
        UUID banco = doubleEntryService.resolveContaBanco(movement);
        UUID debito = par.contaDebitoId();
        UUID credito = par.contaCreditoId();
        if (debito == null && credito != null) {
            debito = banco;
        } else if (credito == null && debito != null) {
            credito = banco;
        }

        // Pre-preenche as DUAS contas no movimento (so quando ainda nao ha nada escolhido),
        // para a tela de conciliacao ja vir com debito e credito sugeridos.
        if (movement.getContaDebitoId() == null && movement.getContaCreditoId() == null
                && (debito != null || credito != null)) {
            movement.setContaDebitoId(debito);
            movement.setContaCreditoId(credito);
            movementRepository.save(movement);
        }

        // A contrapartida (para exibir/aprender) e o lado que nao e o banco.
        UUID contrapartida = isSaida(movement) ? debito : credito;
        return persist(movement, contrapartida, confianca, origem);
    }

    private boolean isSaida(Movement movement) {
        if (movement.getTipo() != null) {
            return movement.getTipo() == com.nalitech.modules.movement.entity.MovementType.SAIDA;
        }
        return movement.getValor() != null && movement.getValor().signum() < 0;
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
