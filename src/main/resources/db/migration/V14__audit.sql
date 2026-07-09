-- E15 - Auditoria

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID,
    empresa_id  UUID,
    acao        VARCHAR(60) NOT NULL,
    entidade    VARCHAR(80),
    entidade_id VARCHAR(80),
    ip          VARCHAR(60),
    user_agent  VARCHAR(300),
    timestamp   TIMESTAMPTZ NOT NULL DEFAULT now(),
    detalhes    JSONB
);

CREATE INDEX idx_audit_logs_empresa ON audit_logs (empresa_id);
CREATE INDEX idx_audit_logs_usuario ON audit_logs (usuario_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp);
