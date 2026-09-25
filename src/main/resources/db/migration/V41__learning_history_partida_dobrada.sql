-- Memoria de classificacao passa a guardar a PARTIDA DOBRADA (debito + credito),
-- para pre-preencher as DUAS contas na proxima conciliacao (nao so a contrapartida).
ALTER TABLE learning_history RENAME COLUMN conta_id TO conta_debito_id;
ALTER TABLE learning_history ALTER COLUMN conta_debito_id DROP NOT NULL;
ALTER TABLE learning_history ADD COLUMN conta_credito_id UUID;
