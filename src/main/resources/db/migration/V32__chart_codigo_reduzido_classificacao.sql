-- Plano de contas: separa o "codigo de classificacao" (mascara hierarquica) do
-- identificador unico da conta.
--
-- Motivo: alguns arquivos (ex.: exportacao com codigo reduzido + codigo de
-- classificacao na mesma coluna, "000519821301001") repetem a MESMA classificacao
-- em varias contas distintas (ex.: dezenas de fornecedores em "21301001"). Usar a
-- classificacao como chave fazia essas contas colidirem numa so. Agora:
--   * codigo               -> identificador UNICO (codigo reduzido quando existir);
--   * codigo_classificacao -> mascara hierarquica (pode repetir; so p/ agrupamento);
--   * codigo_original      -> string completa original, sem remover zeros a esquerda.
--
-- Retrocompatibilidade: planos ja importados usavam o proprio `codigo` como
-- classificacao (nao havia separacao). O backfill copia o valor atual para os novos
-- campos SEM tocar em `codigo` — assim os vinculos de conciliacao (feitos por id) e o
-- indice unico (empresa_id, cliente_id, codigo) continuam validos.

ALTER TABLE chart_of_accounts ADD COLUMN codigo_classificacao VARCHAR(30);
ALTER TABLE chart_of_accounts ADD COLUMN codigo_original       VARCHAR(60);

UPDATE chart_of_accounts SET codigo_classificacao = codigo WHERE codigo_classificacao IS NULL;
UPDATE chart_of_accounts SET codigo_original       = codigo WHERE codigo_original       IS NULL;

-- Consulta por classificacao (agrupamento/hierarquia/relatorios). Nao e unico.
CREATE INDEX idx_chart_codigo_classificacao
    ON chart_of_accounts (empresa_id, codigo_classificacao);
