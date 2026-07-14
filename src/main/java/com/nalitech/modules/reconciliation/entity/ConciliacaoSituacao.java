package com.nalitech.modules.reconciliation.entity;

/**
 * Situacoes de uma Conciliacao (lote/processo mensal por cliente) — spec secao 11.
 * Diferente de {@link ReconciliationStatus}, que e o status de cada item isolado.
 */
public enum ConciliacaoSituacao {
    RASCUNHO,
    AGUARDANDO_ARQUIVO,
    VALIDANDO,
    AGUARDANDO_PARAMETRIZACAO,
    COM_PENDENCIAS,
    PRONTA_PARA_REVISAO,
    EM_REVISAO,
    CONCLUIDA,
    CANCELADA
}
