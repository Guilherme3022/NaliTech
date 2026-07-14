-- EA - Conciliacao por cliente e competencia (spec NALI, secao 12)
--
-- Reconciliation passa a pertencer obrigatoriamente a um cliente e a uma
-- competencia (periodo contabil). As colunas sao criadas como NULL para nao
-- quebrar linhas existentes; a obrigatoriedade e garantida na camada de
-- servico para novos registros.

ALTER TABLE reconciliations ADD COLUMN cliente_id  UUID;
ALTER TABLE reconciliations ADD COLUMN competencia DATE;

CREATE INDEX idx_reconciliations_cliente ON reconciliations (cliente_id, competencia);
