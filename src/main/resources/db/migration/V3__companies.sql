-- E2 - Empresas

CREATE TABLE companies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cnpj                VARCHAR(14) NOT NULL UNIQUE,
    razao_social        VARCHAR(180) NOT NULL,
    inscricao_estadual  VARCHAR(30),
    regime_tributario   VARCHAR(30),
    plano               VARCHAR(30),
    logo_url            VARCHAR(300),
    responsavel_id      UUID,
    status              VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150)
);

-- Empresa padrao do escritorio inicial (mesmo UUID usado pelo DataInitializer).
INSERT INTO companies (id, cnpj, razao_social, regime_tributario, status)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000000191',
        'Escritorio Padrao LedgerFlow', 'SIMPLES_NACIONAL', 'ATIVA');
