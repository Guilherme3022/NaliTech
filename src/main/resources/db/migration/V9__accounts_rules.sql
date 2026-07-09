-- E9 - Plano de contas, motor de regras e aprendizado

CREATE TABLE account_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE TABLE chart_of_accounts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID NOT NULL,
    codigo       VARCHAR(30) NOT NULL,
    nome         VARCHAR(150) NOT NULL,
    tipo         VARCHAR(20),
    category_id  UUID,
    parent_id    UUID,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(150)
);

CREATE INDEX idx_chart_empresa ON chart_of_accounts (empresa_id);
CREATE UNIQUE INDEX idx_chart_empresa_codigo ON chart_of_accounts (empresa_id, codigo);

CREATE TABLE account_rules (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id         UUID NOT NULL,
    nome               VARCHAR(120) NOT NULL,
    descricao_contains VARCHAR(200),
    valor_operador     VARCHAR(5),
    valor_ref          NUMERIC(18,2),
    conta_id           UUID,
    marcar_revisao     BOOLEAN NOT NULL DEFAULT FALSE,
    prioridade         INT NOT NULL DEFAULT 0,
    ativo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         VARCHAR(150)
);

CREATE INDEX idx_account_rules_empresa ON account_rules (empresa_id);

CREATE TABLE ai_suggestions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID NOT NULL,
    movement_id    UUID NOT NULL,
    conta_sugerida UUID,
    confianca      NUMERIC(5,2),
    origem         VARCHAR(20),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(150)
);

CREATE INDEX idx_ai_suggestions_movement ON ai_suggestions (movement_id);

CREATE TABLE learning_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id      UUID NOT NULL,
    descricao_padrao VARCHAR(200) NOT NULL,
    conta_id        UUID NOT NULL,
    ocorrencias     INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150)
);

CREATE INDEX idx_learning_empresa ON learning_history (empresa_id);
