package com.nalitech.modules.file.service;

import com.nalitech.modules.file.dto.UploadDtos.UploadResponse;
import com.nalitech.modules.file.entity.FileEntity;
import com.nalitech.modules.file.entity.Upload;
import com.nalitech.modules.file.entity.UploadStatus;
import com.nalitech.modules.file.event.ArquivoRecebidoEvent;
import com.nalitech.modules.file.repository.FileRepository;
import com.nalitech.modules.file.repository.UploadRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import com.nalitech.shared.storage.StorageService;
import com.nalitech.shared.util.HashUtil;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "csv", "xlsx", "xls", "ofx", "xml", "txt", "zip",
            "jpg", "jpeg", "png", "tiff");

    private final FileRepository fileRepository;
    private final UploadRepository uploadRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public UploadService(FileRepository fileRepository, UploadRepository uploadRepository,
                         StorageService storageService, ApplicationEventPublisher eventPublisher) {
        this.fileRepository = fileRepository;
        this.uploadRepository = uploadRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    public UploadResponse upload(MultipartFile file, UUID clienteId) {
        validate(file);
        UUID empresaId = SecurityUtils.currentEmpresaId();
        byte[] content = readBytes(file);
        String hash = HashUtil.sha256Hex(content);

        fileRepository.findByEmpresaIdAndHashSha256(empresaId, hash).ifPresent(existing -> {
            throw new BusinessException(
                    "Arquivo duplicado: este conteudo ja foi enviado.", HttpStatus.CONFLICT);
        });

        String storageKey = "%s/%s-%s".formatted(empresaId, UUID.randomUUID(), file.getOriginalFilename());
        storageService.store(storageKey, content, file.getContentType());

        FileEntity saved = persistFile(file, empresaId, clienteId, hash, storageKey, content.length);
        Upload upload = persistUpload(empresaId, clienteId, saved.getId());

        eventPublisher.publishEvent(new ArquivoRecebidoEvent(
                upload.getId(), empresaId, saved.getId(), clienteId,
                saved.getTipoMime(), saved.getNomeOriginal()));

        return toResponse(upload, saved);
    }

    @Transactional(readOnly = true)
    public Page<UploadResponse> list(UUID clienteId, OffsetDateTime inicio, OffsetDateTime fim,
                                     Pageable pageable) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        return uploadRepository.filter(empresaId, clienteId, inicio, fim, pageable)
                .map(upload -> toResponse(upload, loadFile(upload.getFileId(), empresaId)));
    }

    @Transactional(readOnly = true)
    public UploadResponse getById(UUID id) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Upload upload = uploadRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload nao encontrado."));
        return toResponse(upload, loadFile(upload.getFileId(), empresaId));
    }

    public void delete(UUID id) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Upload upload = uploadRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload nao encontrado."));
        fileRepository.findByIdAndEmpresaId(upload.getFileId(), empresaId)
                .ifPresent(f -> storageService.delete(f.getStorageKey()));
        uploadRepository.delete(upload);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo vazio.", HttpStatus.BAD_REQUEST);
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Tipo de arquivo nao suportado: " + extension,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private FileEntity persistFile(MultipartFile file, UUID empresaId, UUID clienteId,
                                   String hash, String storageKey, long size) {
        FileEntity entity = new FileEntity();
        entity.setEmpresaId(empresaId);
        entity.setClienteId(clienteId);
        entity.setNomeOriginal(file.getOriginalFilename());
        entity.setTipoMime(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        entity.setTamanho(size);
        entity.setHashSha256(hash);
        entity.setStorageKey(storageKey);
        return fileRepository.save(entity);
    }

    private Upload persistUpload(UUID empresaId, UUID clienteId, UUID fileId) {
        Upload upload = new Upload();
        upload.setEmpresaId(empresaId);
        upload.setClienteId(clienteId);
        upload.setFileId(fileId);
        upload.setStatus(UploadStatus.RECEBIDO);
        return uploadRepository.save(upload);
    }

    private FileEntity loadFile(UUID fileId, UUID empresaId) {
        return fileRepository.findByIdAndEmpresaId(fileId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo nao encontrado."));
    }

    private UploadResponse toResponse(Upload upload, FileEntity file) {
        return new UploadResponse(upload.getId(), file.getId(), upload.getClienteId(),
                file.getNomeOriginal(), file.getTipoMime(), file.getTamanho(),
                upload.getStatus(), upload.getEtapaAtual(), upload.getErroMensagem(),
                upload.getCreatedAt());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("Falha ao ler o arquivo enviado.", HttpStatus.BAD_REQUEST);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
