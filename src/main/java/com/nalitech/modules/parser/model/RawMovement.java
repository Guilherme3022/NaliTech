package com.nalitech.modules.parser.model;

/**
 * Movimentacao crua extraida de um documento, antes da normalizacao.
 *
 * @param data          data no texto original
 * @param valor         valor no texto original (pode conter sinal)
 * @param descricao     historico/descricao
 * @param documento     CNPJ/CPF da contraparte, quando identificado
 * @param tipoIndicador natureza explicita do lancamento quando o layout a informa:
 *                      {@code "D"} (debito/saida) ou {@code "C"} (credito/entrada);
 *                      {@code null} quando o layout nao informa (deriva-se do sinal).
 *                      Atencao: aqui D/C segue a convencao do EXTRATO/movimento
 *                      (D = saida de dinheiro, C = entrada), traduzida em
 *                      {@link com.nalitech.modules.movement.service.MovementNormalizer}.
 */
public record RawMovement(
        String data,
        String valor,
        String descricao,
        String documento,
        String tipoIndicador
) {
    /** Construtor de conveniencia: layout sem indicador explicito de D/C. */
    public RawMovement(String data, String valor, String descricao, String documento) {
        this(data, valor, descricao, documento, null);
    }
}
