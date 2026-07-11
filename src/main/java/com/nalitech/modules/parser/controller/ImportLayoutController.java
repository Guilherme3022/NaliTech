package com.nalitech.modules.parser.controller;

import com.nalitech.modules.parser.dto.ImportLayoutDtos.ImportLayoutRequest;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.ImportLayoutResponse;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.PreviewRequest;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.PreviewResponse;
import com.nalitech.modules.parser.service.ImportLayoutService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/import-layouts")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class ImportLayoutController {

    private final ImportLayoutService importLayoutService;

    public ImportLayoutController(ImportLayoutService importLayoutService) {
        this.importLayoutService = importLayoutService;
    }

    @GetMapping
    public List<ImportLayoutResponse> list() {
        return importLayoutService.list();
    }

    @PostMapping
    public ImportLayoutResponse create(@Valid @RequestBody ImportLayoutRequest request) {
        return importLayoutService.create(request);
    }

    @PutMapping("/{id}")
    public ImportLayoutResponse update(@PathVariable UUID id, @Valid @RequestBody ImportLayoutRequest request) {
        return importLayoutService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        importLayoutService.delete(id);
    }

    /** Pre-visualiza o resultado de um mapeamento aplicado a um CSV colado. */
    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return importLayoutService.preview(request);
    }
}
