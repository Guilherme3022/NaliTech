package com.nalitech.modules.dashboard.service;

import com.nalitech.modules.client.entity.ClientStatus;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.dashboard.dto.DashboardDtos.ActivityItem;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardActivity;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardPortfolio;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardSummary;
import com.nalitech.modules.dashboard.dto.DashboardDtos.OperationSummary;
import com.nalitech.modules.dashboard.dto.DashboardDtos.PortfolioItem;
import com.nalitech.modules.file.entity.UploadStatus;
import com.nalitech.modules.file.repository.UploadRepository;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import java.util.List;
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
    private final ClientRepository clientRepository;

    public DashboardService(UploadRepository uploadRepository,
                            ReconciliationRepository reconciliationRepository,
                            MovementRepository movementRepository,
                            ClientRepository clientRepository) {
        this.uploadRepository = uploadRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
        this.clientRepository = clientRepository;
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

    public OperationSummary operation() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        return new OperationSummary(
                clientRepository.countByEmpresaIdAndStatus(empresaId, ClientStatus.ATIVO),
                reconciliationRepository.countByEmpresaIdAndStatus(empresaId, ReconciliationStatus.PENDENTE),
                movementRepository.countByEmpresaIdAndStatusAndCategoriaSugeridaIsNull(
                        empresaId, MovementStatus.CONCILIADO),
                uploadRepository.countByEmpresaIdAndStatus(empresaId, UploadStatus.PROCESSANDO),
                uploadRepository.countByEmpresaIdAndStatus(empresaId, UploadStatus.ERRO));
    }

    public DashboardPortfolio portfolio() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        List<PortfolioItem> clientes = clientRepository
                .findByEmpresaIdAndStatus(empresaId, ClientStatus.ATIVO).stream()
                .map(cliente -> new PortfolioItem(
                        cliente.getId(),
                        cliente.getNome(),
                        movementRepository.countByEmpresaIdAndClienteIdAndStatus(
                                empresaId, cliente.getId(), MovementStatus.CONCILIACAO_PENDENTE),
                        movementRepository.countByEmpresaIdAndClienteIdAndStatusAndCategoriaSugeridaIsNull(
                                empresaId, cliente.getId(), MovementStatus.CONCILIADO)))
                .sorted((a, b) -> Long.compare(
                        b.pendentesConciliacao() + b.aguardandoClassificacao(),
                        a.pendentesConciliacao() + a.aguardandoClassificacao()))
                .toList();
        return new DashboardPortfolio(clientes);
    }

    public DashboardActivity activity() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        var recentes = uploadRepository.findTop10ByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .map(u -> new ActivityItem(u.getId(), u.getStatus(), u.getEtapaAtual(), u.getCreatedAt()))
                .toList();
        return new DashboardActivity(recentes);
    }
}
