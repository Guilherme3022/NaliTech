-- EC/ED - Conciliacao como lote/processo mensal por cliente (spec secoes 9-12).
-- Agrupa os itens de "reconciliations" por cliente + competencia + perfil.

CREATE TABLE conciliacoes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID NOT NULL,
    cliente_id   UUID NOT NULL,
    competencia  DATE NOT NULL,
    perfil_id    UUID,
    situacao     VARCHAR(30) NOT NULL DEFAULT 'RASCUNHO',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(150)
);

CREATE INDEX idx_conciliacoes_empresa_cliente
    ON conciliacoes (empresa_id, cliente_id, competencia);
