package com.nalitech.modules.reconciliation.dto;

import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.entity.MovementType;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReconciliationDtos {

    private ReconciliationDtos() {
    }

    /** Dados reais de uma movimentacao (extrato ou correspondencia), para conferencia visual. */
    public record MovementView(
            UUID id,
            LocalDate data,
            BigDecimal valor,
            String descricao,
            String documento,
            String banco,
            MovementType tipo,
            MovementStatus status) {
    }

    /** Conta sugerida para o item, com codigo/nome legiveis e a confianca/origem da sugestao. */
    public record SugestaoView(
            UUID contaId,
            String codigo,
            String nome,
            BigDecimal confianca,
            String origem) {
    }

    public record ReconciliationResponse(
            UUID id,
            UUID clienteId,
            LocalDate competencia,
            UUID movementId,
            UUID matchedMovementId,
            ReconciliationStatus status,
            String camada,
            BigDecimal score,
            String motivo,
            // Enriquecimento: o operador ve o que esta confirmando, sem abrir outra tela.
            MovementView movimento,
            MovementView correspondencia,
            SugestaoView sugestao,
            // Pareamento N:1: as movimentacoes do sistema agrupadas contra o extrato.
            List<MovementView> agrupamento) {
    }

    public record ConfirmRequest(UUID contaSugerida) {
    }

    /** Agrupamento N:1: as movimentacoes do sistema que somadas batem com o extrato. */
    public record GroupMatchRequest(@NotEmpty List<UUID> movementIds) {
    }

    /** Item de confirmacao em lote: o id do pareamento e (opcional) a conta escolhida. */
    public record BatchConfirmItem(UUID id, UUID contaSugerida) {
    }

    public record BatchConfirmRequest(@NotEmpty List<BatchConfirmItem> itens) {
    }

    public record BatchRejectRequest(@NotEmpty List<UUID> ids) {
    }

    /** Uma linha do resumo do lote: por status, quantos itens e a soma dos valores. */
    public record SummaryLine(ReconciliationStatus status, long quantidade, BigDecimal valorTotal) {
    }

    public record ReconciliationSummary(long total, BigDecimal valorTotal, List<SummaryLine> porStatus) {
    }
}
