-- V1 baseline: extensoes do Postgres necessarias para o restante do schema.
-- pgcrypto habilita gen_random_uuid(), usado como default de PK nas entidades
-- (todas com id UUID). As tabelas de negocio sao criadas nas migrations dos
-- respectivos epicos (V2 em diante).

CREATE EXTENSION IF NOT EXISTS pgcrypto;
