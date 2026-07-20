-- Pareamento N:1 (agrupamento): uma conciliacao (lancamento do extrato) pode ser
-- casada com varias movimentacoes do sistema que, somadas, batem com o valor do extrato.
-- Cada linha aqui e uma "perna" do agrupamento.

CREATE TABLE reconciliation_matches (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID NOT NULL,
    reconciliation_id UUID NOT NULL,
    movement_id       UUID NOT NULL,
    valor             NUMERIC(18, 2),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(150)
);

CREATE INDEX idx_recon_matches_recon ON reconciliation_matches (reconciliation_id);
CREATE INDEX idx_recon_matches_mov ON reconciliation_matches (movement_id);
