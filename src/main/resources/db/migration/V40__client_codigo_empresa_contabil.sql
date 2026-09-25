-- Codigo da empresa no sistema contabil de destino (ultimo campo do TXT de lancamentos).
-- Numerico, preenchido na criacao/edicao do cliente.
ALTER TABLE clients ADD COLUMN codigo_empresa_contabil INTEGER;
