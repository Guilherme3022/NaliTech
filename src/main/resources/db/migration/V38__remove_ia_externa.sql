-- Remocao completa da IA externa (LLM/sweep) do produto.
--
-- 1) Camada de IA da conciliacao: coluna e indice de controle do sweep.
-- 2) Medicao de consumo de IA por empresa (faturamento).
-- A conciliacao algoritmica e a classificacao deterministica seguem funcionando.

DROP INDEX IF EXISTS idx_reconciliations_ia_pendente;
ALTER TABLE reconciliations DROP COLUMN IF EXISTS ia_tentada;

DROP TABLE IF EXISTS ai_usage_monthly;
