package com.nalitech.modules.file.controller;

import com.nalitech.modules.file.dto.UploadDtos.UploadResponse;
import com.nalitech.modules.file.service.UploadService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "clienteId") UUID clienteId) {
        return uploadService.upload(file, clienteId);
    }

    @GetMapping
    public Page<UploadResponse> list(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            Pageable pageable) {
        return uploadService.list(clienteId, inicio, fim, pageable);
    }

    @GetMapping("/{id}")
    public UploadResponse getById(@PathVariable UUID id) {
        return uploadService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        uploadService.delete(id);
    }
}
