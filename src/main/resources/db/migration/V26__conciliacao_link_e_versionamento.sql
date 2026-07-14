-- ED - vincula itens e arquivos ao lote de conciliacao.
-- EE - versionamento/substituicao de arquivos (spec secoes 17-18).

ALTER TABLE reconciliations ADD COLUMN conciliacao_id UUID;

ALTER TABLE uploads ADD COLUMN conciliacao_id             UUID;
ALTER TABLE uploads ADD COLUMN versao                     INTEGER NOT NULL DEFAULT 1;
ALTER TABLE uploads ADD COLUMN substituido_por_id         UUID;
ALTER TABLE uploads ADD COLUMN justificativa_substituicao VARCHAR(500);

CREATE INDEX idx_reconciliations_conciliacao ON reconciliations (conciliacao_id);
CREATE INDEX idx_uploads_conciliacao ON uploads (conciliacao_id);
