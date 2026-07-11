package com.nalitech.modules.portal.controller;

import com.nalitech.modules.file.dto.UploadDtos.UploadResponse;
import com.nalitech.modules.file.service.UploadService;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/portal")
@PreAuthorize("hasRole('CLIENTE')")
public class PortalController {

    private final UploadService uploadService;

    public PortalController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/uploads", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return uploadService.upload(file, currentClienteId());
    }

    @GetMapping("/status")
    public Page<UploadResponse> status(Pageable pageable) {

        return uploadService.list(currentClienteId(), null, null, pageable);
    }

    private UUID currentClienteId() {
        UUID clienteId = SecurityUtils.requireUser().clienteId();
        if (clienteId == null) {
            throw new BusinessException(
                    "Usuario do portal sem cliente vinculado.", HttpStatus.FORBIDDEN);
        }
        return clienteId;
    }
}
