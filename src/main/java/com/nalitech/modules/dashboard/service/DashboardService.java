package com.nalitech.modules.dashboard.service;

import com.nalitech.modules.dashboard.dto.DashboardDtos.ActivityItem;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardActivity;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardSummary;
import com.nalitech.modules.file.entity.UploadStatus;
import com.nalitech.modules.file.repository.UploadRepository;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
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
