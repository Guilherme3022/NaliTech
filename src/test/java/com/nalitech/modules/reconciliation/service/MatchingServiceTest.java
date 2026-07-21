package com.nalitech.modules.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.service.ClassificationSuggestionService;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingServiceTest {

    @Mock
    private MovementRepository movementRepository;
    @Mock
    private ReconciliationRepository reconciliationRepository;
    @Mock
    private ReconciliationRuleRepository ruleRepository;
    @Mock
    private ClassificationSuggestionService suggestionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MatchingService matchingService;
    private final UUID empresaId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                movementRepository, reconciliationRepository, ruleRepository,
                suggestionService, eventPublisher);
        // Defaults seguros para os caminhos nao exercitados por cada teste.
        when(reconciliationRepository.findFirstByMovementId(any())).thenReturn(Optional.empty());
        when(reconciliationRepository.findByEmpresaIdAndClienteIdAndStatusAndMatchedMovementIdIsNull(
                any(), any(), any())).thenReturn(List.of());
        when(movementRepository.findMatchCandidatesInWindow(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(ruleRepository.findByEmpresaIdAndAtivoTrue(any())).thenReturn(List.of());
    }

    private Movement movimento(UUID uploadId, String origem, String descricao,
                               LocalDate data, BigDecimal valor) {
        Movement movement = new Movement();
        movement.setId(UUID.randomUUID());
        movement.setEmpresaId(empresaId);
        movement.setClienteId(clienteId);
        movement.setUploadId(uploadId);
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
    void casaEntreArquivosDiferentesMesmoSemMarcarPapel() {
        // Os dois arquivos foram enviados como EXTRATO (papel nao marcado) e mesmo assim casa.
        UUID uploadA = UUID.randomUUID();
        UUID uploadB = UUID.randomUUID();
        Movement a = movimento(uploadA, "EXTRATO", "Pagamento Fornecedor X",
                LocalDate.of(2026, 2, 1), new BigDecimal("-100.00"));
        Movement b = movimento(uploadB, "EXTRATO", "Fornecedor X NF 123",
                LocalDate.of(2026, 2, 1), new BigDecimal("-100.00"));
        when(movementRepository.findMatchCandidatesInWindow(eq(empresaId), eq(clienteId),
                eq(uploadA), any(), any())).thenReturn(List.of(b));

        matchingService.reconcile(a);

        Reconciliation saved = captureSaved();
        assertThat(saved.getCamada()).isEqualTo("EXATA");
        assertThat(saved.getMatchedMovementId()).isEqualTo(b.getId());
        assertThat(b.getStatus()).isEqualTo(MovementStatus.CONCILIADO);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void casaComToleranciaDeCentavosQuandoNomeBate() {
        UUID uploadA = UUID.randomUUID();
        UUID uploadB = UUID.randomUUID();
        Movement extrato = movimento(uploadA, "EXTRATO", "PIX RECEBIDO NESTLE BRASIL",
                LocalDate.of(2026, 2, 1), new BigDecimal("-100.00"));
        Movement sistema = movimento(uploadB, "SISTEMA", "NESTLE BRASIL LTDA",
                LocalDate.of(2026, 2, 1), new BigDecimal("-100.40"));
        when(movementRepository.findMatchCandidatesInWindow(eq(empresaId), eq(clienteId),
                eq(uploadA), any(), any())).thenReturn(List.of(sistema));

        matchingService.reconcile(extrato);

        Reconciliation saved = captureSaved();
        assertThat(saved.getCamada()).isEqualTo("APROXIMADA");
        assertThat(saved.getMatchedMovementId()).isEqualTo(sistema.getId());
    }

    @Test
    void naoCasaQuandoValorMuitoDiferente() {
        UUID uploadA = UUID.randomUUID();
        UUID uploadB = UUID.randomUUID();
        Movement a = movimento(uploadA, "EXTRATO", "Compra", LocalDate.of(2026, 2, 1),
                new BigDecimal("-100.00"));
        Movement b = movimento(uploadB, "SISTEMA", "Outro", LocalDate.of(2026, 2, 1),
                new BigDecimal("-130.00")); // 30% de diferenca
        when(movementRepository.findMatchCandidatesInWindow(eq(empresaId), eq(clienteId),
                eq(uploadA), any(), any())).thenReturn(List.of(b));

        matchingService.reconcile(a);

        Reconciliation saved = captureSaved();
        assertThat(saved.getMatchedMovementId()).isNull();
        assertThat(saved.getCamada()).isEqualTo("MANUAL");
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void movimentacaoFechaItemPendenteDeOutroArquivo() {
        UUID uploadA = UUID.randomUUID();
        UUID uploadB = UUID.randomUUID();
        Movement dirigente = movimento(uploadA, "EXTRATO", "PIX NESTLE BRASIL",
                LocalDate.of(2026, 2, 1), new BigDecimal("-100.00"));
        Movement chegando = movimento(uploadB, "SISTEMA", "NESTLE BRASIL LTDA",
                LocalDate.of(2026, 2, 3), new BigDecimal("-100.00"));
        Reconciliation pendente = new Reconciliation();
        pendente.setEmpresaId(empresaId);
        pendente.setClienteId(clienteId);
        pendente.setMovementId(dirigente.getId());
        pendente.setStatus(ReconciliationStatus.PENDENTE);
        when(reconciliationRepository.findByEmpresaIdAndClienteIdAndStatusAndMatchedMovementIdIsNull(
                empresaId, clienteId, ReconciliationStatus.PENDENTE)).thenReturn(List.of(pendente));
        when(movementRepository.findById(dirigente.getId())).thenReturn(Optional.of(dirigente));

        matchingService.reconcile(chegando);

        assertThat(pendente.getMatchedMovementId()).isEqualTo(chegando.getId());
        assertThat(chegando.getStatus()).isEqualTo(MovementStatus.CONCILIADO);
        verify(reconciliationRepository).save(pendente);
    }

    @Test
    void movimentacaoJaConciliadaNaoEReprocessada() {
        UUID uploadA = UUID.randomUUID();
        Movement m = movimento(uploadA, "SISTEMA", "Fornecedor", LocalDate.of(2026, 2, 1),
                new BigDecimal("-100.00"));
        m.setStatus(MovementStatus.CONCILIADO); // ja e contrapartida de um item

        matchingService.reconcile(m);

        verify(reconciliationRepository, never()).save(any());
    }
}
