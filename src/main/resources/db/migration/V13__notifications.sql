-- E14 - Notificacoes

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID NOT NULL,
    destinatario VARCHAR(200) NOT NULL,
    canal        VARCHAR(20) NOT NULL,
    assunto      VARCHAR(200),
    corpo        TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    enviado_em   TIMESTAMPTZ,
    erro         VARCHAR(400),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(150)
);

CREATE INDEX idx_notifications_empresa ON notifications (empresa_id);
