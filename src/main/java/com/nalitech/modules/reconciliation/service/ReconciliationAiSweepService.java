package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.AiSweepJob;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.SweepIaConcluidoEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Varredura por IA sob demanda ("botao"): roda de forma assincrona sobre as
 * pendencias MANUAL que a IA ainda nao avaliou, atualizando um progresso que a
 * tela consulta (barra de carregamento / popup estilo Drive).
 *
 * <p>O registro de jobs e em memoria (MVP single-instance). Basta migrar para
 * Redis quando houver mais de uma instancia.
 */
@Slf4j
@Service
public class ReconciliationAiSweepService {

    private static final String EXECUTANDO = "EXECUTANDO";
    private static final String CONCLUIDO = "CONCLUIDO";
    private static final String ERRO = "ERRO";
    private static final String SEM_PENDENCIAS = "SEM_PENDENCIAS";

    // Quanto tempo manter jobs concluidos visiveis (para o popup reaparecer).
    private static final long RETENCAO_CONCLUIDOS_MS = 30 * 60 * 1000L;

    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationAiItemProcessor itemProcessor;
    private final AiReconciliationMatcher aiMatcher;
    private final ApplicationEventPublisher eventPublisher;
    // Auto-referencia (via proxy) para que @Async em run() seja realmente aplicado
    // — chamada direta this.run(...) ignoraria o proxy do Spring.
    private final ReconciliationAiSweepService self;

    private final ConcurrentHashMap<UUID, JobState> jobs = new ConcurrentHashMap<>();

    public ReconciliationAiSweepService(ReconciliationRepository reconciliationRepository,
                                        ReconciliationAiItemProcessor itemProcessor,
                                        AiReconciliationMatcher aiMatcher,
                                        ApplicationEventPublisher eventPublisher,
                                        @Lazy ReconciliationAiSweepService self) {
        this.reconciliationRepository = reconciliationRepository;
        this.itemProcessor = itemProcessor;
        this.aiMatcher = aiMatcher;
        this.eventPublisher = eventPublisher;
        this.self = self;
    }

    /**
     * Inicia a varredura e devolve o estado inicial do job (com o total). O
     * processamento em si roda em outra thread ({@link #run}).
     */
    public AiSweepJob start(UUID clienteId, LocalDate competencia) {
        if (!aiMatcher.isEnabled()) {
            throw new BusinessException(
                    "A IA de conciliacao esta desligada. Configure RECONCILIATION_AI_ENABLED=true "
                    + "e um provedor (ex.: Ollama local ou Groq) para usar esta funcao.",
                    HttpStatus.BAD_REQUEST);
        }
        UUID empresaId = SecurityUtils.currentEmpresaId();
        List<UUID> pendentes = reconciliationRepository
                .findManualPendingNotAiTried(empresaId, clienteId, competencia)
                .stream()
                .map(Reconciliation::getId)
                .toList();

        JobState state = new JobState(empresaId, clienteId, competencia, pendentes.size());
        if (pendentes.isEmpty()) {
            state.status = SEM_PENDENCIAS;
            state.concluidoEm = OffsetDateTime.now();
        }
        jobs.put(state.jobId, state);
        purgeAntigos();

        if (!pendentes.isEmpty()) {
            self.run(state.jobId, empresaId, pendentes);
        }
        return snapshot(state);
    }

    /** Processa a lista de pendencias em background, item a item (transacao propria). */
    @Async
    public void run(UUID jobId, UUID empresaId, List<UUID> reconciliationIds) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            return;
        }
        try {
            for (UUID id : reconciliationIds) {
                try {
                    boolean resolvido = itemProcessor.process(id, empresaId);
                    if (resolvido) {
                        state.resolvidos.incrementAndGet();
                    }
                } catch (Exception ex) {
                    log.warn("Falha ao avaliar conciliacao {} pela IA: {}", id, ex.getMessage());
                } finally {
                    state.processados.incrementAndGet();
                }
            }
            state.status = CONCLUIDO;
            // Evento interno (sem n8n): notificacao nativa / metricas / auditoria.
            eventPublisher.publishEvent(new SweepIaConcluidoEvent(
                    empresaId, jobId, state.clienteId, state.competencia,
                    state.total, state.resolvidos.get()));
        } catch (Exception ex) {
            log.error("Sweep de IA {} falhou", jobId, ex);
            state.status = ERRO;
        } finally {
            state.concluidoEm = OffsetDateTime.now();
        }
    }

    /** Estado de um job especifico (para a barra de progresso). */
    public AiSweepJob status(UUID jobId) {
        JobState state = jobs.get(jobId);
        if (state == null || !state.empresaId.equals(SecurityUtils.currentEmpresaId())) {
            throw new BusinessException("Job de IA nao encontrado.", HttpStatus.NOT_FOUND);
        }
        return snapshot(state);
    }

    /** Jobs ativos/recentes da empresa (para o popup reaparecer ao navegar). */
    public List<AiSweepJob> active() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        return jobs.values().stream()
                .filter(s -> s.empresaId.equals(empresaId))
                .sorted(Comparator.comparing((JobState s) -> s.iniciadoEm).reversed())
                .map(this::snapshot)
                .toList();
    }

    private void purgeAntigos() {
        long agora = System.currentTimeMillis();
        jobs.values().removeIf(s -> s.concluidoEm != null
                && (agora - s.concluidoEm.toInstant().toEpochMilli()) > RETENCAO_CONCLUIDOS_MS);
    }

    private AiSweepJob snapshot(JobState s) {
        return new AiSweepJob(s.jobId, s.status, s.total, s.processados.get(), s.resolvidos.get(),
                s.clienteId, s.competencia, s.iniciadoEm, s.concluidoEm);
    }

    /** Estado mutavel de um job em memoria. */
    private static final class JobState {
        final UUID jobId = UUID.randomUUID();
        final UUID empresaId;
        final UUID clienteId;
        final LocalDate competencia;
        final int total;
        final OffsetDateTime iniciadoEm = OffsetDateTime.now();
        volatile String status = EXECUTANDO;
        volatile OffsetDateTime concluidoEm;
        final AtomicInteger processados = new AtomicInteger();
        final AtomicInteger resolvidos = new AtomicInteger();

        JobState(UUID empresaId, UUID clienteId, LocalDate competencia, int total) {
            this.empresaId = empresaId;
            this.clienteId = clienteId;
            this.competencia = competencia;
            this.total = total;
        }
    }
}
