-- EC - Perfil de Conciliacao por cliente (spec secao 8).
-- Reune as configuracoes de processamento: sistema de origem, tipo de arquivo,
-- sistema contabil de destino e plano de contas usado.

CREATE TABLE reconciliation_profiles (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id                UUID NOT NULL,
    cliente_id                UUID NOT NULL,
    nome                      VARCHAR(150) NOT NULL,
    sistema_origem            VARCHAR(100),
    tipo_arquivo              VARCHAR(100),
    sistema_contabil_destino  VARCHAR(100),
    plano_id                  UUID,
    ativo                     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                VARCHAR(150)
);

CREATE INDEX idx_reconciliation_profiles_empresa_cliente
    ON reconciliation_profiles (empresa_id, cliente_id);
