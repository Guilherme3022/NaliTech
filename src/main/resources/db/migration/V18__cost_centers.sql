-- Increment 4 - Centro de custo

CREATE TABLE cost_centers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    cliente_id  UUID,
    codigo      VARCHAR(30) NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_cost_centers_empresa ON cost_centers (empresa_id);

-- Centro de custo aplicado ao lancamento e (opcional) definido pela regra De/Para.
ALTER TABLE movements     ADD COLUMN centro_custo_id UUID;
ALTER TABLE account_rules ADD COLUMN centro_custo_id UUID;
