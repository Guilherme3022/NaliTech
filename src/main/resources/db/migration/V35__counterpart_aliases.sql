-- Aprendizado de vinculos manuais: quando o contador confirma um match, guardamos
-- que o nome do extrato e o nome do sistema referem-se a MESMA contraparte (apelido).
-- Assim o match automatico futuro reconhece pares com nomes diferentes (ex.: "SENFF"
-- no extrato x "SENFFNET INSTITUICAO" no sistema). Os nomes sao guardados normalizados
-- e em ordem canonica (nome_a <= nome_b) para busca independente de ordem.
CREATE TABLE counterpart_aliases (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID NOT NULL,
    cliente_id   UUID,
    nome_a       VARCHAR(200) NOT NULL,
    nome_b       VARCHAR(200) NOT NULL,
    ocorrencias  INT NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(150)
);

CREATE INDEX idx_counterpart_aliases_scope ON counterpart_aliases (empresa_id, cliente_id);
CREATE UNIQUE INDEX uq_counterpart_aliases ON counterpart_aliases (empresa_id, cliente_id, nome_a, nome_b);
