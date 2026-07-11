-- Increment 7 - Contratos de emprestimo/financiamento

CREATE TABLE loan_contracts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id           UUID NOT NULL,
    cliente_id           UUID,
    descricao            VARCHAR(150) NOT NULL,
    valor_total          NUMERIC(18,2),
    taxa_juros           NUMERIC(9,4),
    parcelas             INT,
    conta_principal_id   UUID,
    conta_juros_id       UUID,
    conta_encargos_id    UUID,
    classificacao_prazo  VARCHAR(10),   -- CURTO | LONGO
    ativo                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           VARCHAR(150)
);

CREATE INDEX idx_loan_contracts_empresa ON loan_contracts (empresa_id);

-- Vinculo do lancamento ao contrato de financiamento.
ALTER TABLE movements ADD COLUMN loan_contract_id UUID;
