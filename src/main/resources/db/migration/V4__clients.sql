-- E3 - Clientes do escritorio

CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    nome        VARCHAR(180) NOT NULL,
    cnpj_cpf    VARCHAR(14) NOT NULL,
    contato     VARCHAR(120),
    telefone    VARCHAR(200),   -- cifrado em repouso (base64), ver AttributeEncryptor (E16)
    email       VARCHAR(180),
    status      VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_clients_empresa ON clients (empresa_id);
CREATE INDEX idx_clients_nome ON clients (empresa_id, lower(nome));

CREATE TABLE client_documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL,
    cliente_id  UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    file_id     UUID NOT NULL,
    descricao   VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150)
);

CREATE INDEX idx_client_documents_cliente ON client_documents (cliente_id);
