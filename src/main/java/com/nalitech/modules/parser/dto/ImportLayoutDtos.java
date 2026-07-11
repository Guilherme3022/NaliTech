package com.nalitech.modules.parser.dto;

import com.nalitech.modules.parser.model.RawMovement;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public final class ImportLayoutDtos {

    private ImportLayoutDtos() {
    }

    public record ImportLayoutRequest(
            @NotBlank String nome,
            String colData,
            String colValor,
            String colDescricao,
            String colDocumento,
            boolean ativo,
            UUID clienteId) {
    }

    public record ImportLayoutResponse(
            UUID id, String nome, String colData, String colValor, String colDescricao,
            String colDocumento, boolean ativo, UUID clienteId) {
    }

    /** Aplica um mapeamento a um CSV colado, para pre-visualizar o resultado. */
    public record PreviewRequest(
            @NotBlank String conteudo,
            String colData,
            String colValor,
            String colDescricao,
            String colDocumento) {
    }

    public record PreviewResponse(int total, List<RawMovement> linhas) {
    }
}
