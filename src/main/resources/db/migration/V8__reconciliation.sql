-- E8 - Conciliacao bancaria

CREATE TABLE reconciliation_rules (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID NOT NULL,
    nome              VARCHAR(120) NOT NULL,
    descricao_contains VARCHAR(200),
    valor_min         NUMERIC(18,2),
    acao              VARCHAR(30) NOT NULL,
    ativo             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(150)
);

CREATE INDEX idx_reconciliation_rules_empresa ON reconciliation_rules (empresa_id);

CREATE TABLE reconciliations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id          UUID NOT NULL,
    movement_id         UUID NOT NULL,
    matched_movement_id UUID,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    camada              VARCHAR(30),
    score               NUMERIC(5,2),
    motivo              VARCHAR(250),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150)
);

CREATE INDEX idx_reconciliations_empresa_status ON reconciliations (empresa_id, status);
CREATE INDEX idx_reconciliations_movement ON reconciliations (movement_id);

CREATE TABLE movement_matches (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID NOT NULL,
    movement_id  UUID NOT NULL,
    candidate_id UUID NOT NULL,
    score        NUMERIC(5,2),
    tipo_match   VARCHAR(30),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(150)
);

CREATE INDEX idx_movement_matches_movement ON movement_matches (movement_id);
