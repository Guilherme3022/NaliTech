package com.nalitech.modules.reconciliation.entity;

public enum ReconciliationStatus {
    PENDENTE,
    CONFIRMADO,
    REJEITADO,
    // Item que o usuario marcou como "nao precisa conciliar": sai da lista e nao retorna
    // (diferente de REJEITADO, que devolve a movimentacao ao pool para novo match).
    DISPENSADO
}
