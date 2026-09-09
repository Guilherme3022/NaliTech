package com.nalitech.modules.reconciliation.dto;

import com.nalitech.modules.reconciliation.entity.ConciliacaoSituacao;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public final class ConciliacaoDtos {

    private ConciliacaoDtos() {
    }

    public record ConciliacaoResponse(
            UUID id,
            UUID clienteId,
            LocalDate competencia,
            UUID perfilId,
            ConciliacaoSituacao situacao,
            // true enquanto houver algum arquivo anexado ainda em processamento
            // (upload nao finalizado: RECEBIDO/VALIDANDO/PROCESSANDO). O front usa isso
            // para exibir o aviso de processamento e ligar o polling condicional.
            boolean processando) {
    }

    // competencia chega como "YYYY-MM" (input month do front); convertida no controller.
    public record CreateConciliacaoRequest(
            @NotNull UUID clienteId,
            @NotNull String competencia,
            UUID perfilId) {
    }
}
