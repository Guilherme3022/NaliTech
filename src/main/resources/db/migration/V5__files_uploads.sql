-- E4 - Arquivos & Upload

CREATE TABLE document_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(60) NOT NULL UNIQUE,
    extensoes   VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE TABLE files (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID NOT NULL,
    cliente_id     UUID,
    nome_original  VARCHAR(255) NOT NULL,
    tipo_mime      VARCHAR(120) NOT NULL,
    tamanho        BIGINT NOT NULL,
    hash_sha256    VARCHAR(64) NOT NULL,
    storage_key    VARCHAR(300) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(150)
);

CREATE INDEX idx_files_empresa ON files (empresa_id);
CREATE UNIQUE INDEX idx_files_empresa_hash ON files (empresa_id, hash_sha256);

CREATE TABLE uploads (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id    UUID NOT NULL,
    cliente_id    UUID,
    file_id       UUID NOT NULL REFERENCES files (id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'RECEBIDO',
    etapa_atual   VARCHAR(40),
    erro_mensagem VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    VARCHAR(150)
);

CREATE INDEX idx_uploads_empresa ON uploads (empresa_id);
CREATE INDEX idx_uploads_cliente ON uploads (cliente_id);

INSERT INTO document_types (nome, extensoes) VALUES
    ('PDF', 'pdf'),
    ('Planilha', 'csv,xlsx,xls'),
    ('Extrato OFX', 'ofx'),
    ('XML', 'xml'),
    ('Texto', 'txt'),
    ('Compactado', 'zip'),
    ('Imagem', 'jpg,jpeg,png,tiff');
