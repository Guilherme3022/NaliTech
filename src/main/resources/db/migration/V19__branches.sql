-- Increment 5 - Filiais (matriz/filial)

CREATE TABLE branches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    cliente_id  UUID,
    codigo      VARCHAR(30) NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    cnpj        VARCHAR(14),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_branches_empresa ON branches (empresa_id);

-- Filial aplicada ao lancamento e (opcional) definida pela regra De/Para.
ALTER TABLE movements     ADD COLUMN filial_id UUID;
ALTER TABLE account_rules ADD COLUMN filial_id UUID;
