-- EG - Planos-modelo reutilizaveis, POR EMPRESA (spec secao 3).
-- Cada escritorio so enxerga seus proprios modelos; ao cadastrar/atualizar um
-- cliente, um modelo pode ser copiado para o plano de contas do cliente.

CREATE TABLE plano_modelos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    descricao   VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_plano_modelos_empresa ON plano_modelos (empresa_id);

CREATE TABLE plano_modelo_contas (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    modelo_id   UUID NOT NULL,
    codigo      VARCHAR(30) NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    tipo        VARCHAR(20),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_plano_modelo_contas_modelo ON plano_modelo_contas (modelo_id);
