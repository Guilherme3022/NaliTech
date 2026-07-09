package com.ledgerflow.modules.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerflow.modules.movement.entity.Movement;
import com.ledgerflow.modules.movement.entity.MovementStatus;
import com.ledgerflow.modules.movement.repository.MovementRepository;
import com.ledgerflow.modules.reconciliation.entity.Reconciliation;
import com.ledgerflow.modules.reconciliation.entity.ReconciliationRule;
import com.ledgerflow.modules.reconciliation.repository.ReconciliationRepository;
import com.ledgerflow.modules.reconciliation.repository.ReconciliationRuleRepository;
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

    private MatchingService matchingService;
    private final UUID empresaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                movementRepository, reconciliationRepository, ruleRepository, eventPublisher);
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

        Reconciliation result = matchingService.reconcile(alvo);

        assertThat(result.getCamada()).isEqualTo("MANUAL");
        assertThat(result.getMatchedMovementId()).isNull();
        assertThat(alvo.getStatus()).isEqualTo(MovementStatus.CONCILIACAO_PENDENTE);
        verify(eventPublisher, times(1)).publishEvent(any());
    }
}
