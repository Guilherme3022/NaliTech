package com.nalitech.modules.file.entity;

/**
 * Papel de um arquivo enviado dentro da conciliacao.
 *
 * <ul>
 *   <li>{@link #EXTRATO} — lado banco: extrato de conta corrente, gerenciador de
 *       caixa. E o lado que "dirige" a conciliacao (cada item nasce de uma linha
 *       de extrato).</li>
 *   <li>{@link #SISTEMA} — lado contabil/interno: contas a pagar, contas a
 *       receber, livro caixa. Sao os candidatos com que o extrato e casado.</li>
 * </ul>
 */
public enum OrigemDocumento {
    EXTRATO,
    SISTEMA
}
