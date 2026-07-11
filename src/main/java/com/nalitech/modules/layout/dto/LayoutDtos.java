package com.nalitech.modules.layout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class LayoutDtos {

    private LayoutDtos() {
    }

    /** Um lancamento com problema que impede/atrapalha a exportacao. */
    public record ExportIssue(
            UUID movementId, LocalDate data, BigDecimal valor, String descricao, String motivo) {
    }

    /** Relatorio de validacao pre-exportacao. */
    public record ExportValidationReport(int total, int comProblema, List<ExportIssue> problemas) {
    }
}
