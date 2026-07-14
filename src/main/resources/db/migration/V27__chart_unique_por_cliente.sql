-- Spec (plano de contas): o codigo da conta e unico POR CLIENTE (nao por empresa),
-- pois clientes diferentes podem usar a mesma estrutura de codigos.
-- Contas compartilhadas do escritorio (cliente_id nulo) seguem unicas por empresa.

DROP INDEX IF EXISTS idx_chart_empresa_codigo;

CREATE UNIQUE INDEX idx_chart_empresa_cliente_codigo
    ON chart_of_accounts (empresa_id, cliente_id, codigo);

CREATE UNIQUE INDEX idx_chart_empresa_codigo_compartilhado
    ON chart_of_accounts (empresa_id, codigo)
    WHERE cliente_id IS NULL;
