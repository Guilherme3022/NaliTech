package com.nalitech.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record ChartAccountRequest(
            @NotBlank String codigo,
            @NotBlank String nome,
            String tipo,
            UUID categoryId,
            UUID parentId) {
    }

    public record ChartAccountResponse(
            UUID id, String codigo, String nome, String tipo, UUID categoryId, UUID parentId) {
    }

    public record AccountRuleRequest(
            @NotBlank String nome,
            String descricaoContains,
            String valorOperador,
            BigDecimal valorRef,
            UUID contaId,
            boolean marcarRevisao,
            int prioridade,
            boolean ativo) {
    }

    public record AccountRuleResponse(
            UUID id, String nome, String descricaoContains, String valorOperador,
            BigDecimal valorRef, UUID contaId, boolean marcarRevisao, int prioridade, boolean ativo) {
    }

    public record SuggestionResponse(
            UUID movementId, UUID contaSugerida, BigDecimal confianca, String origem) {
    }

    public record ClassifyRequest(UUID contaId) {
    }
}
