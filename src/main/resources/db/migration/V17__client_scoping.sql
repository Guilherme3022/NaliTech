-- Increment 3 - Configuracao por cliente (com fallback para compartilhado).
-- cliente_id NULL = configuracao do escritorio (compartilhada por todos os clientes).
-- cliente_id preenchido = especifica daquele cliente (tem prioridade na resolucao).

ALTER TABLE movements         ADD COLUMN cliente_id UUID;
ALTER TABLE chart_of_accounts ADD COLUMN cliente_id UUID;
ALTER TABLE account_rules     ADD COLUMN cliente_id UUID;
ALTER TABLE bank_accounts     ADD COLUMN cliente_id UUID;
ALTER TABLE learning_history  ADD COLUMN cliente_id UUID;

CREATE INDEX idx_movements_cliente      ON movements (cliente_id);
CREATE INDEX idx_chart_cliente          ON chart_of_accounts (cliente_id);
CREATE INDEX idx_account_rules_cliente  ON account_rules (cliente_id);
CREATE INDEX idx_bank_accounts_cliente  ON bank_accounts (cliente_id);
CREATE INDEX idx_learning_cliente       ON learning_history (cliente_id);
