package com.nalitech.modules.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher.AiMatch;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationRule;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MovementRepository movementRepository;
    @Mock
    private ReconciliationRepository reconciliationRepository;
    @Mock
    private ReconciliationRuleRepository ruleRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AiReconciliationMatcher aiMatcher;

    private MatchingService matchingService;
    private final UUID empresaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                movementRepository, reconciliationRepository, ruleRepository, eventPublisher,
                aiMatcher, 3, new BigDecimal("0.02"), 0.7, 0.5, new BigDecimal("70"), 20, true);
        when(reconciliationRepository.save(any(Reconciliation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Movement movimento(String descricao, LocalDate data, BigDecimal valor) {
        Movement movement = new Movement();
        movement.setId(UUID.randomUUID());
        movement.setEmpresaId(empresaId);
        movement.setDescricao(descricao);
        movement.setData(data);
        movement.setValor(valor);
        movement.setStatus(MovementStatus.NORMALIZADO);
        return movement;
    }

    @Test
    void matchExatoPorDataEValor() {
        Movement alvo = movimento("Pagamento", LocalDate.of(2026, 2, 1), new BigDecimal("100.00"));
        Movement candidato = movimento("Pagamento", LocalDate.of(2026, 2, 1), new BigDecimal("100.00"));
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of(candidato));

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("EXATA");
        assertThat(result.getMatchedMovementId()).isEqualTo(candidato.getId());
        assertThat(result.getScore()).isEqualByComparingTo("100");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void matchPorSimilaridadeDeDescricao() {
        Movement alvo = movimento("Tarifa bancaria", LocalDate.of(2026, 2, 1), new BigDecimal("50.00"));
        Movement candidato = movimento("Tarifa bancaria", LocalDate.of(2026, 3, 9), new BigDecimal("50.00"));
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of());
        when(movementRepository.findByEmpresaIdAndValor(empresaId, alvo.getValor()))
                .thenReturn(List.of(candidato));

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("SIMILARIDADE");
        assertThat(result.getMatchedMovementId()).isEqualTo(candidato.getId());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void matchPorRegraQuandoNaoHaCandidatoDireto() {
        Movement alvo = movimento("Pagamento fornecedor", LocalDate.of(2026, 2, 1), new BigDecimal("300.00"));
        ReconciliationRule rule = new ReconciliationRule();
        rule.setEmpresaId(empresaId);
        rule.setNome("Fornecedores");
        rule.setDescricaoContains("fornecedor");
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of());
        when(movementRepository.findByEmpresaIdAndValor(empresaId, alvo.getValor()))
                .thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId))
                .thenReturn(List.of(rule));

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("REGRA");
        assertThat(result.getMatchedMovementId()).isNull();
        assertThat(result.getMotivo()).contains("Fornecedores");
    }

    @Test
    void semCorrespondenciaGeraPendenciaEPublicaEvento() {
        Movement alvo = movimento("Compra avulsa", LocalDate.of(2026, 2, 1), new BigDecimal("77.00"));
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of());
        when(movementRepository.findByEmpresaIdAndValor(empresaId, alvo.getValor()))
                .thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId))
                .thenReturn(List.of());
        when(movementRepository.findReconciliationCandidates(eq(empresaId), any(), any(), any(), any()))
                .thenReturn(List.of());

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("MANUAL");
        assertThat(result.getMatchedMovementId()).isNull();
        assertThat(alvo.getStatus()).isEqualTo(MovementStatus.CONCILIACAO_PENDENTE);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void matchAproximadoPorToleranciaDeValorEData() {
        Movement alvo = movimento("PIX RECEBIDO JOAO", LocalDate.of(2026, 2, 10), new BigDecimal("200.00"));
        // Mesmo lancamento com 1 centavo de diferenca e 2 dias depois (compensacao).
        Movement candidato = movimento("PIX RECEBIDO JOAO", LocalDate.of(2026, 2, 12), new BigDecimal("200.01"));
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of());
        when(movementRepository.findByEmpresaIdAndValor(empresaId, alvo.getValor()))
                .thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId))
                .thenReturn(List.of());
        when(movementRepository.findReconciliationCandidates(eq(empresaId), any(), any(), any(), any()))
                .thenReturn(List.of(candidato));

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("APROXIMADA");
        assertThat(result.getMatchedMovementId()).isEqualTo(candidato.getId());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void matchPorIaQuandoDemaisCamadasFalham() {
        Movement alvo = movimento("Compra cartao", LocalDate.of(2026, 2, 10), new BigDecimal("90.00"));
        // Descricao bem diferente -> nao casa por similaridade nem aproximada.
        Movement candidato = movimento("Deposito diverso", LocalDate.of(2026, 2, 11), new BigDecimal("90.00"));
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of());
        when(movementRepository.findByEmpresaIdAndValor(empresaId, alvo.getValor()))
                .thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId))
                .thenReturn(List.of());
        when(movementRepository.findReconciliationCandidates(eq(empresaId), any(), any(), any(), any()))
                .thenReturn(List.of(candidato));
        when(aiMatcher.isEnabled()).thenReturn(Boolean.valueOf(true));
        when(aiMatcher.findMatch(eq(alvo), anyList()))
                .thenReturn(java.util.Optional.of(new AiMatch(
                        candidato.getId(), new BigDecimal("88"), "Mesmo valor e data proxima")));

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("IA");
        assertThat(result.getMatchedMovementId()).isEqualTo(candidato.getId());
        assertThat(result.getScore()).isEqualByComparingTo("88");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void iaDesligadaNaoAcionaLlm() {
        Movement alvo = movimento("Servico avulso", LocalDate.of(2026, 2, 10), new BigDecimal("55.00"));
        when(movementRepository.findByEmpresaIdAndDataAndValor(empresaId, alvo.getData(), alvo.getValor()))
                .thenReturn(List.of());
        when(movementRepository.findByEmpresaIdAndValor(empresaId, alvo.getValor()))
                .thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId))
                .thenReturn(List.of());
        when(movementRepository.findReconciliationCandidates(eq(empresaId), any(), any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(aiMatcher.isEnabled()).thenReturn(Boolean.valueOf(false));

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("MANUAL");
        verify(aiMatcher, never()).findMatch(any(), anyList());
    }
}
