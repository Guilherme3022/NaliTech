-- E12 - Financeiro do escritorio (gateway de pagamento)

CREATE TABLE payment_gateway_accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    provider    VARCHAR(30) NOT NULL,
    api_token   VARCHAR(300),
    webhook_secret VARCHAR(200),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_gateway_accounts_empresa ON payment_gateway_accounts (empresa_id);

CREATE TABLE office_fees (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    cliente_id  UUID NOT NULL,
    descricao   VARCHAR(150),
    valor       NUMERIC(18,2) NOT NULL,
    periodicidade VARCHAR(20),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_office_fees_empresa ON office_fees (empresa_id);

CREATE TABLE office_invoices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID NOT NULL,
    cliente_id     UUID NOT NULL,
    fee_id         UUID,
    valor          NUMERIC(18,2) NOT NULL,
    vencimento     DATE NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    provider       VARCHAR(30),
    external_id    VARCHAR(120),
    boleto_url     VARCHAR(400),
    pix_copia_cola VARCHAR(500),
    pix_qrcode     TEXT,
    pago_em        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(150)
);

CREATE INDEX idx_office_invoices_empresa ON office_invoices (empresa_id);
CREATE INDEX idx_office_invoices_external ON office_invoices (provider, external_id);

CREATE TABLE office_receivables (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    invoice_id  UUID NOT NULL,
    valor       NUMERIC(18,2) NOT NULL,
    recebido_em TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE TABLE payment_webhook_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id    UUID,
    provider      VARCHAR(30) NOT NULL,
    external_id   VARCHAR(120) NOT NULL,
    event_type    VARCHAR(60) NOT NULL,
    payload       JSONB,
    processado    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    VARCHAR(150)
);

-- Idempotencia: o mesmo evento (provider + external_id + tipo) so entra uma vez.
CREATE UNIQUE INDEX idx_webhook_events_idempotency
    ON payment_webhook_events (provider, external_id, event_type);
