package com.nalitech.modules.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationRule;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private final UUID clienteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                movementRepository, reconciliationRepository, ruleRepository, eventPublisher);
    }

    private Movement movimento(String origem, String descricao, LocalDate data, BigDecimal valor) {
        Movement movement = new Movement();
        movement.setId(UUID.randomUUID());
        movement.setEmpresaId(empresaId);
        movement.setClienteId(clienteId);
        movement.setOrigem(origem);
        movement.setDescricao(descricao);
        movement.setData(data);
        movement.setValor(valor);
        movement.setStatus(MovementStatus.NORMALIZADO);
        return movement;
    }

    private Reconciliation captureSaved() {
        ArgumentCaptor<Reconciliation> captor = ArgumentCaptor.forClass(Reconciliation.class);
        verify(reconciliationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void extratoCasaComSistemaMesmaDataEValor() {
        Movement extrato = movimento("EXTRATO", "Pagamento Fornecedor X", LocalDate.of(2026, 2, 1),
                new BigDecimal("-100.00"));
        Movement sistema = movimento("SISTEMA", "Fornecedor X NF 123", LocalDate.of(2026, 2, 1),
                new BigDecimal("-100.00"));
        when(movementRepository.findSistemaCandidates(eq(empresaId), eq(clienteId),
                eq(extrato.getValor()), any(), any())).thenReturn(List.of(sistema));
        when(reconciliationRepository.existsByMatchedMovementId(sistema.getId())).thenReturn(false);

        matchingService.reconcile(extrato);

        Reconciliation saved = captureSaved();
        assertThat(saved.getCamada()).isEqualTo("EXATA");
        assertThat(saved.getMatchedMovementId()).isEqualTo(sistema.getId());
        assertThat(sistema.getStatus()).isEqualTo(MovementStatus.CONCILIADO);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void extratoCasaComSistemaDentroDaJanelaDeDatas() {
        Movement extrato = movimento("EXTRATO", "Tarifa", LocalDate.of(2026, 2, 5),
                new BigDecimal("-50.00"));
        Movement sistema = movimento("SISTEMA", "Tarifa", LocalDate.of(2026, 2, 2),
                new BigDecimal("-50.00"));
        when(movementRepository.findSistemaCandidates(eq(empresaId), eq(clienteId),
                eq(extrato.getValor()), any(), any())).thenReturn(List.of(sistema));

        matchingService.reconcile(extrato);

        Reconciliation saved = captureSaved();
        assertThat(saved.getCamada()).isEqualTo("APROXIMADA");
        assertThat(saved.getMatchedMovementId()).isEqualTo(sistema.getId());
    }

    @Test
    void extratoSemCandidatoUsaRegra() {
        Movement extrato = movimento("EXTRATO", "Pagamento fornecedor", LocalDate.of(2026, 2, 1),
                new BigDecimal("-300.00"));
        ReconciliationRule rule = new ReconciliationRule();
        rule.setEmpresaId(empresaId);
        rule.setNome("Fornecedores");
        rule.setDescricaoContains("fornecedor");
        when(movementRepository.findSistemaCandidates(eq(empresaId), eq(clienteId),
                eq(extrato.getValor()), any(), any())).thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId)).thenReturn(List.of(rule));

        matchingService.reconcile(extrato);

        Reconciliation saved = captureSaved();
        assertThat(saved.getCamada()).isEqualTo("REGRA");
        assertThat(saved.getMatchedMovementId()).isNull();
        assertThat(saved.getMotivo()).contains("Fornecedores");
    }

    @Test
    void extratoSemCorrespondenciaGeraPendenciaEPublicaEvento() {
        Movement extrato = movimento("EXTRATO", "Compra avulsa", LocalDate.of(2026, 2, 1),
                new BigDecimal("-77.00"));
        when(movementRepository.findSistemaCandidates(eq(empresaId), eq(clienteId),
                eq(extrato.getValor()), any(), any())).thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(empresaId)).thenReturn(List.of());

        matchingService.reconcile(extrato);

        Reconciliation saved = captureSaved();
        assertThat(saved.getCamada()).isEqualTo("MANUAL");
        assertThat(saved.getMatchedMovementId()).isNull();
        assertThat(extrato.getStatus()).isEqualTo(MovementStatus.CONCILIACAO_PENDENTE);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void sistemaPreencheItemDeExtratoPendente() {
        Movement extrato = movimento("EXTRATO", "Fornecedor X", LocalDate.of(2026, 2, 1),
                new BigDecimal("-100.00"));
        Movement sistema = movimento("SISTEMA", "Fornecedor X", LocalDate.of(2026, 2, 3),
                new BigDecimal("-100.00"));
        Reconciliation pendente = new Reconciliation();
        pendente.setEmpresaId(empresaId);
        pendente.setClienteId(clienteId);
        pendente.setMovementId(extrato.getId());
        pendente.setStatus(ReconciliationStatus.PENDENTE);
        when(reconciliationRepository.findByEmpresaIdAndClienteIdAndStatusAndMatchedMovementIdIsNull(
                empresaId, clienteId, ReconciliationStatus.PENDENTE)).thenReturn(List.of(pendente));
        when(movementRepository.findById(extrato.getId())).thenReturn(Optional.of(extrato));

        matchingService.reconcile(sistema);

        assertThat(pendente.getMatchedMovementId()).isEqualTo(sistema.getId());
        assertThat(sistema.getStatus()).isEqualTo(MovementStatus.CONCILIADO);
        verify(reconciliationRepository).save(pendente);
    }
}
