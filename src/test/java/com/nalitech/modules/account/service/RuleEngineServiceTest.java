package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.repository.AccountRuleRepository;
import com.nalitech.modules.movement.entity.Movement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private AccountRuleRepository ruleRepository;

    private RuleEngineService ruleEngine;
    private final UUID empresaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngineService(ruleRepository);
    }

    private Movement movimento(String descricao, BigDecimal valor) {
        Movement movement = new Movement();
        movement.setEmpresaId(empresaId);
        movement.setDescricao(descricao);
        movement.setValor(valor);
        return movement;
    }

    private AccountRule regra(String contains, String operador, BigDecimal valorRef) {
        AccountRule rule = new AccountRule();
        rule.setEmpresaId(empresaId);
        rule.setNome("regra");
        rule.setDescricaoContains(contains);
        rule.setValorOperador(operador);
        rule.setValorRef(valorRef);
        rule.setContaId(UUID.randomUUID());
        return rule;
    }

    @Test
    void casaPorDescricaoIgnorandoCaixa() {
        AccountRule rule = regra("tarifa", null, null);
        when(ruleRepository.findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(empresaId))
                .thenReturn(List.of(rule));

        var match = ruleEngine.firstMatching(movimento("TARIFA bancaria mensal", null));

        assertThat(match).contains(rule);
    }

    @Test
    void naoCasaQuandoDescricaoNaoContemOTermo() {
        AccountRule rule = regra("pix", null, null);
        when(ruleRepository.findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(empresaId))
                .thenReturn(List.of(rule));

        var match = ruleEngine.firstMatching(movimento("Compra no cartao", null));

        assertThat(match).isEmpty();
    }

    @Test
    void aplicaOperadorDeValorMaiorQue() {
        AccountRule rule = regra(null, "GT", new BigDecimal("100.00"));
        when(ruleRepository.findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(empresaId))
                .thenReturn(List.of(rule));

        assertThat(ruleEngine.firstMatching(movimento("x", new BigDecimal("150.00")))).contains(rule);
        assertThat(ruleEngine.firstMatching(movimento("x", new BigDecimal("50.00")))).isEmpty();
    }

    @Test
    void usaValorAbsolutoNaComparacao() {
        AccountRule rule = regra(null, "GT", new BigDecimal("100.00"));
        when(ruleRepository.findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(empresaId))
                .thenReturn(List.of(rule));

        assertThat(ruleEngine.firstMatching(movimento("x", new BigDecimal("-150.00")))).contains(rule);
    }

    @Test
    void retornaAPrimeiraRegraQueCasaRespeitandoAOrdemRecebida() {
        AccountRule maisPrioritaria = regra("pix", null, null);
        AccountRule menosPrioritaria = regra("pix", null, null);
        when(ruleRepository.findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(empresaId))
                .thenReturn(List.of(maisPrioritaria, menosPrioritaria));

        var match = ruleEngine.firstMatching(movimento("PIX recebido", null));

        assertThat(match).contains(maisPrioritaria);
    }

    @Test
    void semRegrasNaoHaMatch() {
        when(ruleRepository.findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(empresaId))
                .thenReturn(List.of());

        assertThat(ruleEngine.firstMatching(movimento("qualquer", null))).isEmpty();
    }
}
