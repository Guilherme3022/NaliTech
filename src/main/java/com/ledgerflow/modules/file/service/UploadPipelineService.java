package com.ledgerflow.modules.file.service;

import com.ledgerflow.modules.file.entity.FileEntity;
import com.ledgerflow.modules.file.entity.Upload;
import com.ledgerflow.modules.file.entity.UploadStatus;
import com.ledgerflow.modules.file.event.ArquivoRecebidoEvent;
import com.ledgerflow.modules.file.event.UploadErroEvent;
import com.ledgerflow.modules.file.event.UploadProcessadoEvent;
import com.ledgerflow.modules.file.repository.FileRepository;
import com.ledgerflow.modules.file.repository.UploadRepository;
import com.ledgerflow.modules.movement.event.MovimentacoesNormalizadasEvent;
import com.ledgerflow.modules.movement.service.MovementNormalizer;
import com.ledgerflow.modules.ocr.entity.OcrResult;
import com.ledgerflow.modules.ocr.service.OcrService;
import com.ledgerflow.modules.parser.DocumentParserFactory;
import com.ledgerflow.modules.parser.model.ParseResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.ledgerflow.shared.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class UploadPipelineService {

    private final UploadRepository uploadRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final DocumentParserFactory parserFactory;
    private final MovementNormalizer movementNormalizer;
    private final ApplicationEventPublisher eventPublisher;

    public UploadPipelineService(UploadRepository uploadRepository, FileRepository fileRepository,
                                 StorageService storageService, OcrService ocrService,
                                 DocumentParserFactory parserFactory,
                                 MovementNormalizer movementNormalizer,
                                 ApplicationEventPublisher eventPublisher) {
        this.uploadRepository = uploadRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.ocrService = ocrService;
        this.parserFactory = parserFactory;
        this.movementNormalizer = movementNormalizer;
        this.eventPublisher = eventPublisher;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void handle(ArquivoRecebidoEvent event) {
        Upload upload = uploadRepository.findById(event.uploadId()).orElse(null);
        if (upload == null) {
            return;
        }
        try {
            FileEntity file = fileRepository.findById(event.fileId()).orElseThrow();
            byte[] content = storageService.retrieve(file.getStorageKey());
            String extension = extensionOf(file.getNomeOriginal());

            advance(upload, UploadStatus.VALIDANDO, "OCR");
            byte[] parserInput = prepareInput(extension, event, file, content);

            advance(upload, UploadStatus.PROCESSANDO, "PARSER");
            ParseResult parseResult = parserFactory.resolve(extension).parse(parserInput);

            List<UUID> movementIds = movementNormalizer.normalize(
                    upload.getId(), event.empresaId(), extension, parseResult.movements());

            advance(upload, UploadStatus.CONCLUIDO, "NORMALIZACAO");
            eventPublisher.publishEvent(new MovimentacoesNormalizadasEvent(
                    upload.getId(), event.empresaId(), movementIds));
            eventPublisher.publishEvent(new UploadProcessadoEvent(
                    upload.getId(), event.empresaId(), movementIds.size()));
        } catch (Exception ex) {
            log.error("Falha no pipeline do upload {}", upload.getId(), ex);
            upload.marcarErro(upload.getEtapaAtual(), ex.getMessage());
            uploadRepository.save(upload);
            eventPublisher.publishEvent(new UploadErroEvent(
                    upload.getId(), event.empresaId(), upload.getEtapaAtual(), ex.getMessage()));
        }
    }

    private byte[] prepareInput(String extension, ArquivoRecebidoEvent event, FileEntity file,
                                byte[] content) {
        boolean precisaOcr = "pdf".equals(extension) || file.getTipoMime().startsWith("image");
        if (precisaOcr) {
            OcrResult ocr = ocrService.process(
                    event.uploadId(), event.empresaId(), file.getTipoMime(), content);
            return ocr.getTextoExtraido() == null
                    ? new byte[0]
                    : ocr.getTextoExtraido().getBytes(StandardCharsets.UTF_8);
        }
        return content;
    }

    private void advance(Upload upload, UploadStatus status, String etapa) {
        upload.avancar(status, etapa);
        uploadRepository.save(upload);
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
