package com.nalitech.modules.dashboard.dto;

import com.nalitech.modules.file.entity.UploadStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSummary(
            long conciliacoesPendentes,
            long uploadsHoje,
            long uploadsComErro,
            long movimentacoesConciliadas) {
    }

    public record ActivityItem(
            UUID uploadId,
            UploadStatus status,
            String etapaAtual,
            OffsetDateTime quando) {
    }

    public record DashboardActivity(List<ActivityItem> recentes) {
    }
}
