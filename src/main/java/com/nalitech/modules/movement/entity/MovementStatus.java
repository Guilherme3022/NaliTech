package com.nalitech.modules.movement.entity;

public enum MovementStatus {
    NORMALIZADO,
    CONCILIACAO_PENDENTE,
    CONCILIADO,
    CLASSIFICADO,
    // Movimentacao dispensada pelo usuario ("nao precisa conciliar"): estado terminal,
    // nao reentra no matching/reprocess nem entra no arquivo de exportacao.
    IGNORADO
}
