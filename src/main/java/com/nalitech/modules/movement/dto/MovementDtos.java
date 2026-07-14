package com.nalitech.modules.movement.dto;

import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.entity.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class MovementDtos {

    private MovementDtos() {
    }

    public record MovementResponse(
            UUID id,
            UUID clienteId,
            LocalDate data,
            BigDecimal valor,
            String descricao,
            MovementType tipo,
            String documento,
            String banco,
            UUID contaDebitoId,
            UUID contaCreditoId,
            MovementStatus status) {
    }

    // Edicao manual de uma movimentacao (corrigir leitura, ajustar conta).
    public record UpdateMovementRequest(
            LocalDate data,
            BigDecimal valor,
            String descricao,
            String documento,
            UUID contaDebitoId,
            UUID contaCreditoId) {
    }
}
