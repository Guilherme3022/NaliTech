package com.nalitech.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record ChartAccountRequest(
            @NotBlank String codigo,
            // Opcionais: quando ausentes, assumem o valor de `codigo` (contas com codigo unico).
            String codigoClassificacao,
            String codigoOriginal,
            @NotBlank String nome,
            String tipo,
            Boolean analitica,
            String naturezaSaldo,
            UUID categoryId,
            UUID parentId,
            UUID clienteId) {
    }

    public record ChartAccountResponse(
            UUID id, String codigo, String codigoClassificacao, String codigoOriginal,
            String nome, String tipo, Boolean analitica, String naturezaSaldo,
            UUID categoryId, UUID parentId, UUID clienteId) {
    }

    public record AccountRuleRequest(
            @NotBlank String nome,
            String descricaoContains,
            String valorOperador,
            BigDecimal valorRef,
            UUID contaId,
            boolean marcarRevisao,
            int prioridade,
            boolean ativo,
            UUID clienteId,
            UUID centroCustoId,
            UUID filialId,
            String tipoMovimento,
            String bancoContains,
            String documentoContains) {
    }

    public record AccountRuleResponse(
            UUID id, String nome, String descricaoContains, String valorOperador,
            BigDecimal valorRef, UUID contaId, boolean marcarRevisao, int prioridade, boolean ativo,
            UUID clienteId, UUID centroCustoId, UUID filialId,
            String tipoMovimento, String bancoContains, String documentoContains) {
    }

    public record SuggestionResponse(
            UUID movementId, UUID contaSugerida, BigDecimal confianca, String origem) {
    }

    public record ClassifyRequest(UUID contaId) {
    }

    /** Item da fila de "Solicitação de Parametrização": um padrão ainda sem De/Para. */
    public record ParametrizationRequest(
            String descricaoPadrao, String exemplo, long ocorrencias, BigDecimal valorTotal) {
    }

    /** Aplica um De/Para em lote a todas as movimentações pendentes que casam com o termo. */
    public record ApplyParametrizationRequest(
            @NotBlank String descricaoContains,
            @NotNull UUID contaId,
            boolean criarRegra) {
    }

    public record ApplyParametrizationResponse(int classificados, boolean regraCriada) {
    }

    /** Ajuste manual do lancamento de partida dobrada. */
    public record ManualEntryRequest(
            @NotNull UUID contaDebitoId,
            @NotNull UUID contaCreditoId) {
    }

    public record BankAccountRequest(
            @NotBlank String nome,
            @NotNull UUID contaContabilId,
            boolean padrao,
            UUID clienteId) {
    }

    public record BankAccountResponse(
            UUID id, String nome, UUID contaContabilId, boolean padrao, UUID clienteId) {
    }

    /** Atribuição manual de centro de custo a um lançamento. */
    public record CostCenterAssignRequest(UUID centroCustoId) {
    }

    public record CostCenterRequest(
            @NotBlank String codigo,
            @NotBlank String nome,
            boolean ativo,
            UUID clienteId) {
    }

    public record CostCenterResponse(
            UUID id, String codigo, String nome, boolean ativo, UUID clienteId) {
    }

    /** Atribuição manual de filial a um lançamento. */
    public record BranchAssignRequest(UUID filialId) {
    }

    public record BranchRequest(
            @NotBlank String codigo,
            @NotBlank String nome,
            String cnpj,
            boolean ativo,
            UUID clienteId) {
    }

    public record BranchResponse(
            UUID id, String codigo, String nome, String cnpj, boolean ativo, UUID clienteId) {
    }

    /** Vinculo manual de um lancamento a um contrato de financiamento. */
    public record LoanContractAssignRequest(UUID loanContractId) {
    }

    public record LoanContractRequest(
            @NotBlank String descricao,
            BigDecimal valorTotal,
            BigDecimal taxaJuros,
            Integer parcelas,
            UUID contaPrincipalId,
            UUID contaJurosId,
            UUID contaEncargosId,
            String classificacaoPrazo,
            boolean ativo,
            UUID clienteId) {
    }

    public record LoanContractResponse(
            UUID id, String descricao, BigDecimal valorTotal, BigDecimal taxaJuros, Integer parcelas,
            UUID contaPrincipalId, UUID contaJurosId, UUID contaEncargosId,
            String classificacaoPrazo, boolean ativo, UUID clienteId) {
    }
}
