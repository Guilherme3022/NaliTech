-- Increment 6 - Regras De/Para mais ricas

ALTER TABLE account_rules ADD COLUMN tipo_movimento     VARCHAR(10); -- ENTRADA | SAIDA | null (qualquer)
ALTER TABLE account_rules ADD COLUMN banco_contains     VARCHAR(120);
ALTER TABLE account_rules ADD COLUMN documento_contains VARCHAR(120);
