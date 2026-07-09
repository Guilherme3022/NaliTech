-- E7 - Normalizacao (modelo unico de movimentacao)

CREATE TABLE movements (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id         UUID NOT NULL,
    upload_id          UUID NOT NULL,
    data               DATE,
    valor              NUMERIC(18,2),
    descricao          VARCHAR(300),
    tipo               VARCHAR(10),
    origem             VARCHAR(60),
    documento          VARCHAR(80),
    banco              VARCHAR(80),
    categoria_sugerida UUID,
    status             VARCHAR(25) NOT NULL DEFAULT 'NORMALIZADO',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         VARCHAR(150)
);

CREATE INDEX idx_movements_empresa ON movements (empresa_id);
CREATE INDEX idx_movements_upload ON movements (upload_id);
CREATE INDEX idx_movements_status ON movements (empresa_id, status);
CREATE INDEX idx_movements_data_valor ON movements (empresa_id, data, valor);
