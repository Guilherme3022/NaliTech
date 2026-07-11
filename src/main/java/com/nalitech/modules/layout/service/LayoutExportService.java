package com.nalitech.modules.layout.service;

import com.nalitech.modules.audit.Audited;
import com.nalitech.modules.file.entity.FileEntity;
import com.nalitech.modules.file.repository.FileRepository;
import com.nalitech.modules.layout.dto.LayoutDtos.ExportIssue;
import com.nalitech.modules.layout.dto.LayoutDtos.ExportValidationReport;
import com.nalitech.modules.layout.entity.LayoutExport;
import com.nalitech.modules.layout.event.ExportacaoGeradaEvent;
import com.nalitech.modules.account.entity.Branch;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.entity.CostCenter;
import com.nalitech.modules.account.repository.BranchRepository;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.account.repository.CostCenterRepository;
import com.nalitech.modules.layout.exporter.ExportContext;
import com.nalitech.modules.layout.exporter.ExportedFile;
import com.nalitech.modules.layout.exporter.LayoutExporterFactory;
import com.nalitech.modules.layout.repository.LayoutExportRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.storage.StorageService;
import com.nalitech.shared.util.HashUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final ChartOfAccountRepository chartRepository;
    private final CostCenterRepository costCenterRepository;
    private final BranchRepository branchRepository;

    public LayoutExportService(LayoutExporterFactory exporterFactory,
                               MovementRepository movementRepository,
                               LayoutExportRepository exportRepository,
                               FileRepository fileRepository,
                               StorageService storageService,
                               ApplicationEventPublisher eventPublisher,
                               ChartOfAccountRepository chartRepository,
                               CostCenterRepository costCenterRepository,
                               BranchRepository branchRepository) {
        this.exporterFactory = exporterFactory;
        this.movementRepository = movementRepository;
        this.exportRepository = exportRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.chartRepository = chartRepository;
        this.costCenterRepository = costCenterRepository;
        this.branchRepository = branchRepository;
    }

    public List<String> sistemasSuportados() {
        return exporterFactory.sistemasSuportados();
    }

    @Audited(action = "EXPORTACAO", entity = "LAYOUT")
    public ExportedFile export(String sistema, LocalDate inicio, LocalDate fim, UUID filialId) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        List<Movement> movements = movementRepository
                .findByEmpresaIdAndDataBetweenAndStatusIn(empresaId, inicio, fim, EXPORTAVEIS);
        // Filial: se informada, gera arquivo individual daquela filial; senao, consolidado.
        if (filialId != null) {
            movements = movements.stream()
                    .filter(m -> filialId.equals(m.getFilialId()))
                    .toList();
        }

        ExportContext context = buildContext(empresaId);
        ExportedFile exported = exporterFactory.resolve(sistema).export(movements, context);
        UUID fileId = persistFile(empresaId, exported);
        persistExportRecord(empresaId, sistema, inicio, fim, fileId, movements.size());

        eventPublisher.publishEvent(new ExportacaoGeradaEvent(empresaId, sistema, inicio, fim, fileId));
        return exported;
    }

    /** Valida os lancamentos do periodo antes de exportar (partida dobrada incompleta). */
    @Transactional(readOnly = true)
    public ExportValidationReport validate(LocalDate inicio, LocalDate fim, UUID filialId) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        List<Movement> movements = movementRepository
                .findByEmpresaIdAndDataBetweenAndStatusIn(empresaId, inicio, fim, EXPORTAVEIS);
        if (filialId != null) {
            movements = movements.stream().filter(m -> filialId.equals(m.getFilialId())).toList();
        }
        List<ExportIssue> problemas = movements.stream()
                .map(this::validar)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ExportValidationReport(movements.size(), problemas.size(), problemas);
    }

    private ExportIssue validar(Movement m) {
        String motivo = null;
        if (m.getContaDebitoId() == null && m.getContaCreditoId() == null) {
            motivo = "Lancamento sem conta contabil (nao classificado / sem De/Para).";
        } else if (m.getContaDebitoId() == null) {
            motivo = "Lancamento sem conta de debito.";
        } else if (m.getContaCreditoId() == null) {
            motivo = "Lancamento sem conta de credito.";
        }
        return motivo == null ? null
                : new ExportIssue(m.getId(), m.getData(), m.getValor(), m.getDescricao(), motivo);
    }

    private ExportContext buildContext(UUID empresaId) {
        Map<UUID, String> codigosConta = chartRepository.findByEmpresaId(empresaId).stream()
                .collect(Collectors.toMap(ChartOfAccount::getId, ChartOfAccount::getCodigo,
                        (existente, novo) -> existente));
        Map<UUID, String> codigosCentroCusto = costCenterRepository.findByEmpresaId(empresaId).stream()
                .collect(Collectors.toMap(CostCenter::getId, CostCenter::getCodigo,
                        (existente, novo) -> existente));
        Map<UUID, String> codigosFilial = branchRepository.findByEmpresaId(empresaId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getCodigo,
                        (existente, novo) -> existente));
        return new ExportContext(codigosConta, codigosCentroCusto, codigosFilial);
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
