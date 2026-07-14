package com.nalitech.modules.reconciliation.controller;

import com.nalitech.modules.reconciliation.dto.ConciliacaoDtos.ConciliacaoResponse;
import com.nalitech.modules.reconciliation.dto.ConciliacaoDtos.CreateConciliacaoRequest;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService.ExportFile;
import com.nalitech.modules.reconciliation.service.ConciliacaoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conciliacoes")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class ConciliacaoController {

    private final ConciliacaoService conciliacaoService;
    private final ConciliacaoExportService exportService;

    public ConciliacaoController(ConciliacaoService conciliacaoService,
                                 ConciliacaoExportService exportService) {
        this.conciliacaoService = conciliacaoService;
        this.exportService = exportService;
    }

    @GetMapping
    public List<ConciliacaoResponse> list(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) String competencia) {
        return conciliacaoService.list(clienteId, parseCompetencia(competencia));
    }

    @GetMapping("/{id}")
    public ConciliacaoResponse getById(@PathVariable UUID id) {
        return conciliacaoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConciliacaoResponse create(@Valid @RequestBody CreateConciliacaoRequest request) {
        return conciliacaoService.create(request.clienteId(), parseCompetencia(request.competencia()),
                request.perfilId());
    }

    @PostMapping("/{id}/uploads/{uploadId}")
    public ConciliacaoResponse attachUpload(@PathVariable UUID id, @PathVariable UUID uploadId) {
        return conciliacaoService.attachUpload(id, uploadId);
    }

    @PostMapping("/{id}/concluir")
    public ConciliacaoResponse concluir(@PathVariable UUID id) {
        return conciliacaoService.concluir(id);
    }

    @PostMapping("/{id}/cancelar")
    public ConciliacaoResponse cancelar(@PathVariable UUID id) {
        return conciliacaoService.cancelar(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "TXT") String formato) {
        ExportFile file = exportService.export(id, formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    // "YYYY-MM" -> 1o dia do mes.
    private LocalDate parseCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return null;
        }
        return LocalDate.parse(competencia.trim() + "-01");
    }
}
