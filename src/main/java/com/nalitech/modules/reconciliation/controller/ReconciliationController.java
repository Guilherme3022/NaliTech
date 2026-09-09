package com.nalitech.modules.reconciliation.controller;

import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ConfirmRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.AiSweepJob;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationResponse;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReprocessResponse;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.service.ReconciliationAiSweepService;
import com.nalitech.modules.reconciliation.service.ReconciliationService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reconciliations")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final ReconciliationAiSweepService aiSweepService;

    public ReconciliationController(ReconciliationService reconciliationService,
                                   ReconciliationAiSweepService aiSweepService) {
        this.reconciliationService = reconciliationService;
        this.aiSweepService = aiSweepService;
    }

    @GetMapping("/pending")
    public Page<ReconciliationResponse> pending(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) String competencia,
            Pageable pageable) {
        return reconciliationService.pending(clienteId, parseCompetencia(competencia), pageable);
    }

    @GetMapping("/history")
    public Page<ReconciliationResponse> history(
            @RequestParam(defaultValue = "CONFIRMADO") ReconciliationStatus status,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) String competencia,
            Pageable pageable) {
        return reconciliationService.history(status, clienteId, parseCompetencia(competencia), pageable);
    }

    // Competencia chega como "YYYY-MM" (input month do front) -> 1o dia do mes.
    private LocalDate parseCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return null;
        }
        return LocalDate.parse(competencia.trim() + "-01");
    }

    @PostMapping("/{id}/confirm")
    public ReconciliationResponse confirm(@PathVariable UUID id,
                                          @RequestBody(required = false) ConfirmRequest request) {
        UUID contaSugerida = request == null ? null : request.contaSugerida();
        return reconciliationService.confirm(id, contaSugerida);
    }

    @PostMapping("/{id}/reject")
    public ReconciliationResponse reject(@PathVariable UUID id) {
        return reconciliationService.reject(id);
    }

    // Re-roda o matching nas pendencias sem correspondencia (MANUAL), aplicando
    // novas regras e/ou a IA de conciliacao. Filtros opcionais por cliente/competencia.
    @PostMapping("/reprocess")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
    public ReprocessResponse reprocess(@RequestParam(required = false) UUID clienteId,
                                       @RequestParam(required = false) String competencia) {
        return reconciliationService.reprocessPending(clienteId, parseCompetencia(competencia));
    }

    // Dispara a varredura por IA (assincrona) das pendencias MANUAL que faltam.
    // Devolve o job com o total; a tela acompanha o progresso pelos GETs abaixo.
    @PostMapping("/ai-sweep")
    public AiSweepJob startAiSweep(@RequestParam(required = false) UUID clienteId,
                                   @RequestParam(required = false) String competencia) {
        return aiSweepService.start(clienteId, parseCompetencia(competencia));
    }

    // Progresso de um job especifico (barra de carregamento).
    @GetMapping("/ai-sweep/{jobId}")
    public AiSweepJob aiSweepStatus(@PathVariable UUID jobId) {
        return aiSweepService.status(jobId);
    }

    // Jobs ativos/recentes da empresa (popup que reaparece ao navegar).
    @GetMapping("/ai-sweep")
    public List<AiSweepJob> aiSweepActive() {
        return aiSweepService.active();
    }
}
