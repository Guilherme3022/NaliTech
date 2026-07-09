package com.ledgerflow.modules.account.service;

import com.ledgerflow.modules.account.entity.AccountRule;
import com.ledgerflow.modules.account.repository.AccountRuleRepository;
import com.ledgerflow.modules.movement.entity.Movement;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RuleEngineService {

    private final AccountRuleRepository ruleRepository;

    public RuleEngineService(AccountRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public Optional<AccountRule> firstMatching(Movement movement) {
        return ruleRepository
                .findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(movement.getEmpresaId())
                .stream()
                .filter(rule -> matches(rule, movement))
                .findFirst();
    }

    private boolean matches(AccountRule rule, Movement movement) {
        return descricaoMatches(rule, movement) && valorMatches(rule, movement);
    }

    private boolean descricaoMatches(AccountRule rule, Movement movement) {
        if (rule.getDescricaoContains() == null || rule.getDescricaoContains().isBlank()) {
            return true;
        }
        return movement.getDescricao() != null
                && movement.getDescricao().toLowerCase()
                    .contains(rule.getDescricaoContains().toLowerCase());
    }

    private boolean valorMatches(AccountRule rule, Movement movement) {
        if (rule.getValorOperador() == null || rule.getValorRef() == null || movement.getValor() == null) {
            return true;
        }
        BigDecimal valor = movement.getValor().abs();
        int comparison = valor.compareTo(rule.getValorRef());
        return switch (rule.getValorOperador().toUpperCase()) {
            case "GT" -> comparison > 0;
            case "LT" -> comparison < 0;
            case "EQ" -> comparison == 0;
            default -> true;
        };
    }
}
