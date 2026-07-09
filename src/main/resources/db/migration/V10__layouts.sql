-- E10 - Layouts / Exportacao

CREATE TABLE layout_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    sistema     VARCHAR(30) NOT NULL,
    campos      JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_layout_templates_empresa ON layout_templates (empresa_id);

CREATE TABLE layout_exports (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID NOT NULL,
    sistema        VARCHAR(30) NOT NULL,
    periodo_inicio DATE,
    periodo_fim    DATE,
    file_id        UUID,
    quantidade     INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(150)
);

CREATE INDEX idx_layout_exports_empresa ON layout_exports (empresa_id);
