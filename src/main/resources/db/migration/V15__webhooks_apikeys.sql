-- E20 - Webhooks de saida e API keys (integracao com n8n)

CREATE TABLE webhook_subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    evento      VARCHAR(40) NOT NULL,
    url_destino VARCHAR(500) NOT NULL,
    segredo     VARCHAR(120) NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_webhook_subs_empresa_evento ON webhook_subscriptions (empresa_id, evento);

CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id      UUID NOT NULL,
    subscription_id UUID NOT NULL,
    evento          VARCHAR(40) NOT NULL,
    payload         JSONB,
    http_status     INT,
    tentativa       INT NOT NULL DEFAULT 1,
    sucesso         BOOLEAN NOT NULL DEFAULT FALSE,
    erro            VARCHAR(400),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150)
);

CREATE INDEX idx_webhook_deliveries_sub ON webhook_deliveries (subscription_id);

CREATE TABLE api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    chave_hash  VARCHAR(120) NOT NULL UNIQUE,
    escopos     VARCHAR(300),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_uso  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_api_keys_empresa ON api_keys (empresa_id);
