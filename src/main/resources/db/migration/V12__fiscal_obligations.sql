-- E13 - Agenda fiscal

CREATE TABLE fiscal_obligations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    cliente_id  UUID,
    tipo        VARCHAR(80) NOT NULL,
    descricao   VARCHAR(200),
    vencimento  DATE NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_fiscal_obligations_empresa ON fiscal_obligations (empresa_id);
CREATE INDEX idx_fiscal_obligations_vencimento ON fiscal_obligations (empresa_id, vencimento);
