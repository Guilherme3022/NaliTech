-- Natureza estrutural da conta: analitica (lancavel) x sintetica (agrupadora).
-- Usada pela conciliacao e pelas sugestoes para so oferecer contas analiticas.
-- true = analitica, false = sintetica, NULL = indefinida (tratada como lancavel).

ALTER TABLE chart_of_accounts   ADD COLUMN analitica BOOLEAN;
ALTER TABLE plano_modelo_contas ADD COLUMN analitica BOOLEAN;

-- 1) Backfill pelo rotulo textual ja gravado em "tipo" (S/A, sintetica/analitica, 1/2).
UPDATE chart_of_accounts SET analitica = TRUE
 WHERE analitica IS NULL
   AND upper(trim(tipo)) IN ('A', 'ANALITICA', 'ANALÍTICA', 'ANALITICO', '2');
UPDATE chart_of_accounts SET analitica = FALSE
 WHERE analitica IS NULL
   AND upper(trim(tipo)) IN ('S', 'SINTETICA', 'SINTÉTICA', 'SINTETICO', '1', 'T', 'TOTAL');

-- 2) Backfill pela hierarquia do codigo: e sintetica quando e prefixo de outra conta do
--    mesmo escopo (empresa + cliente). As folhas restantes viram analiticas.
UPDATE chart_of_accounts c SET analitica = FALSE
 WHERE c.analitica IS NULL
   AND EXISTS (
       SELECT 1 FROM chart_of_accounts c2
        WHERE c2.empresa_id = c.empresa_id
          AND coalesce(c2.cliente_id::text, '') = coalesce(c.cliente_id::text, '')
          AND c2.codigo <> c.codigo
          AND c2.codigo LIKE c.codigo || '%');
UPDATE chart_of_accounts SET analitica = TRUE WHERE analitica IS NULL;

-- 3) Mesmo criterio de hierarquia para os planos-modelo (escopo = modelo).
UPDATE plano_modelo_contas c SET analitica = FALSE
 WHERE c.analitica IS NULL
   AND EXISTS (
       SELECT 1 FROM plano_modelo_contas c2
        WHERE c2.modelo_id = c.modelo_id
          AND c2.codigo <> c.codigo
          AND c2.codigo LIKE c.codigo || '%');
UPDATE plano_modelo_contas SET analitica = TRUE WHERE analitica IS NULL;
