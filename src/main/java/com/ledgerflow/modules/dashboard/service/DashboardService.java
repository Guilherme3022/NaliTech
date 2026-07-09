package com.ledgerflow.modules.dashboard.service;

import com.ledgerflow.modules.dashboard.dto.DashboardDtos.ActivityItem;
import com.ledgerflow.modules.dashboard.dto.DashboardDtos.DashboardActivity;
import com.ledgerflow.modules.dashboard.dto.DashboardDtos.DashboardSummary;
import com.ledgerflow.modules.file.entity.UploadStatus;
import com.ledgerflow.modules.file.repository.UploadRepository;
import com.ledgerflow.modules.movement.entity.MovementStatus;
import com.ledgerflow.modules.movement.repository.MovementRepository;
import com.ledgerflow.modules.reconciliation.entity.ReconciliationStatus;
import com.ledgerflow.modules.reconciliation.repository.ReconciliationRepository;
import com.ledgerflow.security.SecurityUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UploadRepository uploadRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final MovementRepository movementRepository;

    public DashboardService(UploadRepository uploadRepository,
                            ReconciliationRepository reconciliationRepository,
                            MovementRepository movementRepository) {
        this.uploadRepository = uploadRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
    }

    public DashboardSummary summary() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        OffsetDateTime inicioDoDia = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        return new DashboardSummary(
                reconciliationRepository.countByEmpresaIdAndStatus(empresaId, ReconciliationStatus.PENDENTE),
                uploadRepository.countByEmpresaIdAndCreatedAtAfter(empresaId, inicioDoDia),
                uploadRepository.countByEmpresaIdAndStatus(empresaId, UploadStatus.ERRO),
                movementRepository.countByEmpresaIdAndStatus(empresaId, MovementStatus.CONCILIADO));
    }

    public DashboardActivity activity() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        var recentes = uploadRepository.findTop10ByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .map(u -> new ActivityItem(u.getId(), u.getStatus(), u.getEtapaAtual(), u.getCreatedAt()))
                .toList();
        return new DashboardActivity(recentes);
    }
}
