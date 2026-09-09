-- Medicao de consumo de IA por empresa (para faturamento / repasse na mensalidade).
--
-- Agregado MENSAL por empresa e feature (CONCILIACAO, CLASSIFICACAO...). Incremento
-- atomico via UPSERT (ON CONFLICT), duravel a restart (ao contrario das metricas
-- Prometheus, que zeram). O Prometheus continua servindo para dashboard operacional.

CREATE TABLE ai_usage_monthly (
    id              UUID PRIMARY KEY,
    empresa_id      UUID        NOT NULL,
    ano_mes         VARCHAR(7)  NOT NULL,       -- competencia de uso: YYYY-MM
    feature         VARCHAR(40) NOT NULL,       -- CONCILIACAO, CLASSIFICACAO, ...
    chamadas        BIGINT      NOT NULL DEFAULT 0,
    tokens_entrada  BIGINT      NOT NULL DEFAULT 0,
    tokens_saida    BIGINT      NOT NULL DEFAULT 0,
    atualizado_em   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ai_usage_empresa_mes_feature UNIQUE (empresa_id, ano_mes, feature)
);

CREATE INDEX idx_ai_usage_empresa_mes ON ai_usage_monthly (empresa_id, ano_mes);
