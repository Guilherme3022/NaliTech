package com.nalitech.modules.file.service;

import com.nalitech.modules.file.dto.UploadDtos.UploadResponse;
import com.nalitech.modules.file.entity.FileEntity;
import com.nalitech.modules.file.entity.OrigemDocumento;
import com.nalitech.modules.file.entity.Upload;
import com.nalitech.modules.file.entity.UploadStatus;
import com.nalitech.modules.file.event.ArquivoRecebidoEvent;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.file.repository.FileRepository;
import com.nalitech.modules.file.repository.UploadRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.ConciliacaoSituacao;
import com.nalitech.modules.reconciliation.repository.ConciliacaoRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationMatchRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import java.util.List;
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
    private final ClientRepository clientRepository;
    private final ConciliacaoRepository conciliacaoRepository;
    private final MovementRepository movementRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationMatchRepository matchRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public UploadService(FileRepository fileRepository, UploadRepository uploadRepository,
                         ClientRepository clientRepository, ConciliacaoRepository conciliacaoRepository,
                         MovementRepository movementRepository,
                         ReconciliationRepository reconciliationRepository,
                         ReconciliationMatchRepository matchRepository,
                         StorageService storageService, ApplicationEventPublisher eventPublisher) {
        this.fileRepository = fileRepository;
        this.uploadRepository = uploadRepository;
        this.clientRepository = clientRepository;
        this.conciliacaoRepository = conciliacaoRepository;
        this.movementRepository = movementRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.matchRepository = matchRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * EE (spec 17-18): substitui um arquivo criando uma nova versao e preservando
     * o original no historico (com a justificativa). Nunca ha exclusao definitiva.
     */
    public UploadResponse substitute(UUID id, MultipartFile file, String justificativa) {
        validate(file);
        UUID empresaId = SecurityUtils.requireEmpresaId();
        Upload original = uploadRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload nao encontrado."));

        byte[] content = readBytes(file);
        String hash = HashUtil.sha256Hex(content);
        fileRepository.findByEmpresaIdAndHashSha256(empresaId, hash).ifPresent(existing -> {
            throw new BusinessException(
                    "Arquivo duplicado: este conteudo ja foi enviado.", HttpStatus.CONFLICT);
        });
        String storageKey = "%s/%s-%s".formatted(empresaId, UUID.randomUUID(), file.getOriginalFilename());
        storageService.store(storageKey, content, file.getContentType());
        FileEntity saved = persistFile(file, empresaId, original.getClienteId(), hash, storageKey, content.length);

        Upload novo = new Upload();
        novo.setEmpresaId(empresaId);
        novo.setClienteId(original.getClienteId());
        novo.setFileId(saved.getId());
        novo.setConciliacaoId(original.getConciliacaoId());
        novo.setOrigem(original.getOrigem());
        novo.setBankAccountId(original.getBankAccountId());
        novo.setVersao(original.getVersao() + 1);
        novo.setStatus(UploadStatus.RECEBIDO);
        Upload persistedNovo = uploadRepository.save(novo);

        original.setSubstituidoPorId(persistedNovo.getId());
        original.setJustificativaSubstituicao(justificativa);
        uploadRepository.save(original);

        eventPublisher.publishEvent(new ArquivoRecebidoEvent(
                persistedNovo.getId(), empresaId, saved.getId(), original.getClienteId(),
                nome(original.getOrigem()), saved.getTipoMime(), saved.getNomeOriginal()));
        return toResponse(persistedNovo, saved);
    }

    public UploadResponse upload(MultipartFile file, UUID clienteId, OrigemDocumento origem,
                                 UUID bankAccountId) {
        validate(file);
        UUID empresaId = SecurityUtils.requireEmpresaId();
        OrigemDocumento papel = origem != null ? origem : OrigemDocumento.EXTRATO;
        // Item 9: arquivo obrigatoriamente vinculado a um cliente da empresa atual.
        if (clienteId == null) {
            throw new BusinessException(
                    "Selecione um cliente antes de enviar o arquivo.", HttpStatus.BAD_REQUEST);
        }
        clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));
        byte[] content = readBytes(file);
        String hash = HashUtil.sha256Hex(content);

        fileRepository.findByEmpresaIdAndHashSha256(empresaId, hash).ifPresent(existing -> {
            throw new BusinessException(
                    "Arquivo duplicado: este conteudo ja foi enviado.", HttpStatus.CONFLICT);
        });

        String storageKey = "%s/%s-%s".formatted(empresaId, UUID.randomUUID(), file.getOriginalFilename());
        storageService.store(storageKey, content, file.getContentType());

        FileEntity saved = persistFile(file, empresaId, clienteId, hash, storageKey, content.length);
        Upload upload = persistUpload(empresaId, clienteId, saved.getId(), papel, bankAccountId);

        eventPublisher.publishEvent(new ArquivoRecebidoEvent(
                upload.getId(), empresaId, saved.getId(), clienteId,
                papel.name(), saved.getTipoMime(), saved.getNomeOriginal()));

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
        // EE: arquivo de conciliacao concluida nao pode ser excluido (rastreabilidade).
        if (upload.getConciliacaoId() != null
                && conciliacaoRepository.findByIdAndEmpresaId(upload.getConciliacaoId(), empresaId)
                        .map(c -> c.getSituacao() == ConciliacaoSituacao.CONCLUIDA)
                        .orElse(false)) {
            throw new BusinessException(
                    "Arquivo de conciliacao concluida nao pode ser excluido. "
                    + "Use a substituicao (mantem o historico).", HttpStatus.BAD_REQUEST);
        }
        // Limpeza em cascata: remove as movimentacoes extraidas deste arquivo e os
        // itens de conciliacao gerados a partir delas (senao ficam "fantasmas" no
        // dashboard mesmo depois de apagar o upload).
        List<Movement> movimentos = movementRepository.findByUploadId(upload.getId());
        if (!movimentos.isEmpty()) {
            List<UUID> movimentoIds = movimentos.stream().map(Movement::getId).toList();

            // Se este arquivo era o EXTRATO, os itens de conciliacao reservaram
            // movimentacoes do SISTEMA (status CONCILIADO). Libera essas reservas.
            for (var recon : reconciliationRepository.findByMovementIdIn(movimentoIds)) {
                if (recon.getMatchedMovementId() != null) {
                    movementRepository.findById(recon.getMatchedMovementId()).ifPresent(sistema -> {
                        sistema.setStatus(MovementStatus.NORMALIZADO);
                        movementRepository.save(sistema);
                    });
                }
            }
            // Se este arquivo era o SISTEMA, desfaz a associacao nos itens de extrato
            // que apontavam para estas movimentacoes (voltam a ficar pendentes).
            for (var recon : reconciliationRepository.findByMatchedMovementIdIn(movimentoIds)) {
                recon.setMatchedMovementId(null);
                recon.setCamada("MANUAL");
                recon.setMotivo("Correspondencia removida (arquivo do sistema excluido)");
                reconciliationRepository.save(recon);
            }

            matchRepository.deleteByMovementIdIn(movimentoIds);
            reconciliationRepository.deleteByMovementIdIn(movimentoIds);
            movementRepository.deleteAll(movimentos);
        }
        // Remove o upload primeiro e faz flush: a FK uploads.file_id -> files e
        // ON DELETE CASCADE, entao o `file` precisa sair depois (senao o cascade do
        // banco apagaria o upload junto e o delete explicito afetaria 0 linhas).
        UUID fileId = upload.getFileId();
        uploadRepository.delete(upload);
        uploadRepository.flush();
        // Agora remove o arquivo tanto do storage (binario no MinIO) quanto da tabela
        // `files` (metadados), para nao deixar o registro orfao no banco.
        fileRepository.findByIdAndEmpresaId(fileId, empresaId)
                .ifPresent(f -> {
                    storageService.delete(f.getStorageKey());
                    fileRepository.delete(f);
                });
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

    private Upload persistUpload(UUID empresaId, UUID clienteId, UUID fileId, OrigemDocumento origem,
                                 UUID bankAccountId) {
        Upload upload = new Upload();
        upload.setEmpresaId(empresaId);
        upload.setClienteId(clienteId);
        upload.setFileId(fileId);
        upload.setOrigem(origem);
        upload.setBankAccountId(bankAccountId);
        upload.setStatus(UploadStatus.RECEBIDO);
        return uploadRepository.save(upload);
    }

    private String nome(OrigemDocumento origem) {
        return (origem != null ? origem : OrigemDocumento.EXTRATO).name();
    }

    private FileEntity loadFile(UUID fileId, UUID empresaId) {
        return fileRepository.findByIdAndEmpresaId(fileId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo nao encontrado."));
    }

    private UploadResponse toResponse(Upload upload, FileEntity file) {
        return new UploadResponse(upload.getId(), file.getId(), upload.getClienteId(),
                upload.getOrigem(), upload.getBankAccountId(), file.getNomeOriginal(),
                file.getTipoMime(), file.getTamanho(),
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
