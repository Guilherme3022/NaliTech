package com.nalitech.modules.reconciliation.controller;

import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.BatchConfirmRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.BatchRejectRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.BatchDispensarRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ConfirmRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.DispensarRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.GroupMatchRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationResponse;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationSummary;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReprocessResponse;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService.ExportFile;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService.ExportOptions;
import com.nalitech.modules.reconciliation.service.ReconciliationService;
import com.nalitech.security.SecurityUtils;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ConciliacaoExportService exportService;

    public ReconciliationController(ReconciliationService reconciliationService,
                                   ConciliacaoExportService exportService) {
        this.reconciliationService = reconciliationService;
        this.exportService = exportService;
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

    // Acoes em lote: confirma/rejeita varios itens numa unica chamada.
    @PostMapping("/confirm-batch")
    public List<ReconciliationResponse> confirmBatch(@Valid @RequestBody BatchConfirmRequest request) {
        return reconciliationService.confirmMany(request.itens());
    }

    @PostMapping("/reject-batch")
    public List<ReconciliationResponse> rejectBatch(@Valid @RequestBody BatchRejectRequest request) {
        return reconciliationService.rejectMany(request.ids());
    }

    // "Nao conciliar": dispensa um item que nao precisa ser conciliado (sai da lista e nao volta).
    @PostMapping("/{id}/dispensar")
    public ReconciliationResponse dispensar(@PathVariable UUID id,
                                            @RequestBody(required = false) DispensarRequest request) {
        String motivo = request == null ? null : request.motivo();
        return reconciliationService.dispensar(id, motivo);
    }

    @PostMapping("/dispensar-batch")
    public List<ReconciliationResponse> dispensarBatch(@Valid @RequestBody BatchDispensarRequest request) {
        return reconciliationService.dispensarMany(request.ids(), request.motivo());
    }

    // Pareamento N:1: agrupa varias movimentacoes do sistema contra o lancamento do extrato.
    @PostMapping("/{id}/group-match")
    public ReconciliationResponse groupMatch(@PathVariable UUID id,
                                             @Valid @RequestBody GroupMatchRequest request) {
        return reconciliationService.groupMatch(id, request);
    }

    // Otimizacao global do match dos itens pendentes (atribuicao otima aproximada).
    @PostMapping("/optimize")
    public void optimize(@RequestParam UUID clienteId,
                         @RequestParam String competencia) {
        reconciliationService.optimize(clienteId, parseCompetencia(competencia));
    }

    // Resumo do lote (por status: quantidade e soma dos valores).
    @GetMapping("/summary")
    public ReconciliationSummary summary(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) String competencia) {
        return reconciliationService.summary(clienteId, parseCompetencia(competencia));
    }

    // Re-roda o matching nas pendencias sem correspondencia (MANUAL), aplicando
    // novas regras. Filtros opcionais por cliente/competencia.
    @PostMapping("/reprocess")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
    public ReprocessResponse reprocess(@RequestParam(required = false) UUID clienteId,
                                       @RequestParam(required = false) String competencia) {
        return reconciliationService.reprocessPending(clienteId, parseCompetencia(competencia));
    }

    // Gera o arquivo de lancamentos (TXT/CSV) por cliente/competencia a QUALQUER momento
    // (nao exige conciliacao concluida), para lancar no sistema contabil.
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam UUID clienteId,
            @RequestParam String competencia,
            @RequestParam(defaultValue = "TXT") String formato,
            @RequestParam(defaultValue = "false") boolean somenteConciliados,
            @RequestParam(defaultValue = ".") String separadorDecimal,
            @RequestParam(defaultValue = "false") boolean incluirCabecalho,
            @RequestParam(defaultValue = "true") boolean incluirSaldoAnterior,
            @RequestParam(defaultValue = "UTF-8") String encoding) {
        char decimal = separadorDecimal != null && separadorDecimal.startsWith(",") ? ',' : '.';
        ExportOptions opts = new ExportOptions(formato, decimal, encoding,
                incluirCabecalho, somenteConciliados, incluirSaldoAnterior);
        ExportFile file = exportService.exportCompetencia(
                SecurityUtils.currentEmpresaId(), clienteId, parseCompetencia(competencia), opts);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
