package com.ledgerflow.modules.layout.service;

import com.ledgerflow.modules.audit.Audited;
import com.ledgerflow.modules.file.entity.FileEntity;
import com.ledgerflow.modules.file.repository.FileRepository;
import com.ledgerflow.modules.layout.entity.LayoutExport;
import com.ledgerflow.modules.layout.event.ExportacaoGeradaEvent;
import com.ledgerflow.modules.layout.exporter.ExportedFile;
import com.ledgerflow.modules.layout.exporter.LayoutExporterFactory;
import com.ledgerflow.modules.layout.repository.LayoutExportRepository;
import com.ledgerflow.modules.movement.entity.Movement;
import com.ledgerflow.modules.movement.entity.MovementStatus;
import com.ledgerflow.modules.movement.repository.MovementRepository;
import com.ledgerflow.security.SecurityUtils;
import com.ledgerflow.shared.storage.StorageService;
import com.ledgerflow.shared.util.HashUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LayoutExportService {

    private static final List<MovementStatus> EXPORTAVEIS =
            List.of(MovementStatus.CONCILIADO, MovementStatus.CLASSIFICADO);

    private final LayoutExporterFactory exporterFactory;
    private final MovementRepository movementRepository;
    private final LayoutExportRepository exportRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public LayoutExportService(LayoutExporterFactory exporterFactory,
                               MovementRepository movementRepository,
                               LayoutExportRepository exportRepository,
                               FileRepository fileRepository,
                               StorageService storageService,
                               ApplicationEventPublisher eventPublisher) {
        this.exporterFactory = exporterFactory;
        this.movementRepository = movementRepository;
        this.exportRepository = exportRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    public List<String> sistemasSuportados() {
        return exporterFactory.sistemasSuportados();
    }

    @Audited(action = "EXPORTACAO", entity = "LAYOUT")
    public ExportedFile export(String sistema, LocalDate inicio, LocalDate fim) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        List<Movement> movements = movementRepository
                .findByEmpresaIdAndDataBetweenAndStatusIn(empresaId, inicio, fim, EXPORTAVEIS);

        ExportedFile exported = exporterFactory.resolve(sistema).export(movements);
        UUID fileId = persistFile(empresaId, exported);
        persistExportRecord(empresaId, sistema, inicio, fim, fileId, movements.size());

        eventPublisher.publishEvent(new ExportacaoGeradaEvent(empresaId, sistema, inicio, fim, fileId));
        return exported;
    }

    @Transactional(readOnly = true)
    public Page<LayoutExport> history(Pageable pageable) {
        return exportRepository.findByEmpresaId(SecurityUtils.currentEmpresaId(), pageable);
    }

    private UUID persistFile(UUID empresaId, ExportedFile exported) {
        String storageKey = "%s/exports/%s-%s".formatted(empresaId, UUID.randomUUID(), exported.filename());
        storageService.store(storageKey, exported.content(), exported.contentType());

        FileEntity file = new FileEntity();
        file.setEmpresaId(empresaId);
        file.setNomeOriginal(exported.filename());
        file.setTipoMime(exported.contentType());
        file.setTamanho(exported.content().length);
        file.setHashSha256(HashUtil.sha256Hex(exported.content()));
        file.setStorageKey(storageKey);
        return fileRepository.save(file).getId();
    }

    private void persistExportRecord(UUID empresaId, String sistema, LocalDate inicio,
                                     LocalDate fim, UUID fileId, int quantidade) {
        LayoutExport export = new LayoutExport();
        export.setEmpresaId(empresaId);
        export.setSistema(sistema);
        export.setPeriodoInicio(inicio);
        export.setPeriodoFim(fim);
        export.setFileId(fileId);
        export.setQuantidade(quantidade);
        exportRepository.save(export);
    }
}
