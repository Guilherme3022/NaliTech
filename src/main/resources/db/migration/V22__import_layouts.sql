-- Increment 9 (Parte B) - Layouts de importacao (mapeamento campo -> coluna)

CREATE TABLE import_layouts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID NOT NULL,
    cliente_id     UUID,
    nome           VARCHAR(120) NOT NULL,
    col_data       VARCHAR(120),
    col_valor      VARCHAR(120),
    col_descricao  VARCHAR(120),
    col_documento  VARCHAR(120),
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(150)
);

CREATE INDEX idx_import_layouts_empresa ON import_layouts (empresa_id);
