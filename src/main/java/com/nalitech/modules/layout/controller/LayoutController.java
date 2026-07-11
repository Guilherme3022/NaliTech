package com.nalitech.modules.layout.controller;

import com.nalitech.modules.layout.entity.LayoutExport;
import com.nalitech.modules.layout.exporter.ExportedFile;
import com.nalitech.modules.layout.service.LayoutExportService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/layouts")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class LayoutController {

    private final LayoutExportService layoutExportService;

    public LayoutController(LayoutExportService layoutExportService) {
        this.layoutExportService = layoutExportService;
    }

    @GetMapping
    public List<String> sistemas() {
        return layoutExportService.sistemasSuportados();
    }

    @PostMapping("/{sistema}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String sistema,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID filialId) {
        ExportedFile file = layoutExportService.export(sistema, inicio, fim, filialId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @GetMapping("/exports/history")
    public Page<LayoutExport> history(Pageable pageable) {
        return layoutExportService.history(pageable);
    }
}
