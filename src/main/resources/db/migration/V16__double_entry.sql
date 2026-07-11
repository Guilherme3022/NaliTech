-- Increment 2 - Partida dobrada (debito/credito) + contas bancarias

-- Lancamento contabil de dupla entrada em cada movimentacao.
ALTER TABLE movements ADD COLUMN conta_debito_id  UUID;
ALTER TABLE movements ADD COLUMN conta_credito_id UUID;

-- Contas bancarias cadastradas (o outro lado do lancamento). Varias por empresa;
-- uma (ou mais) marcada como padrao. A resolucao pega a primeira padrao.
CREATE TABLE bank_accounts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id         UUID NOT NULL,
    nome               VARCHAR(120) NOT NULL,
    conta_contabil_id  UUID NOT NULL,
    padrao             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         VARCHAR(150)
);

CREATE INDEX idx_bank_accounts_empresa ON bank_accounts (empresa_id);
