package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.repository.AccountRuleRepository;
import com.nalitech.modules.movement.entity.Movement;
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
                .findApplicable(movement.getEmpresaId(), movement.getClienteId())
                .stream()
                .filter(rule -> matches(rule, movement))
                .findFirst();
    }

    private boolean matches(AccountRule rule, Movement movement) {
        return descricaoMatches(rule, movement)
                && valorMatches(rule, movement)
                && tipoMatches(rule, movement)
                && bancoMatches(rule, movement)
                && documentoMatches(rule, movement);
    }

    private boolean tipoMatches(AccountRule rule, Movement movement) {
        if (rule.getTipoMovimento() == null || rule.getTipoMovimento().isBlank()) {
            return true;
        }
        return movement.getTipo() != null
                && rule.getTipoMovimento().equalsIgnoreCase(movement.getTipo().name());
    }

    private boolean bancoMatches(AccountRule rule, Movement movement) {
        if (rule.getBancoContains() == null || rule.getBancoContains().isBlank()) {
            return true;
        }
        return movement.getBanco() != null
                && movement.getBanco().toLowerCase().contains(rule.getBancoContains().toLowerCase());
    }

    private boolean documentoMatches(AccountRule rule, Movement movement) {
        if (rule.getDocumentoContains() == null || rule.getDocumentoContains().isBlank()) {
            return true;
        }
        return movement.getDocumento() != null
                && movement.getDocumento().toLowerCase()
                    .contains(rule.getDocumentoContains().toLowerCase());
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
