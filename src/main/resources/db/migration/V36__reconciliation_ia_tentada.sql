-- Camada de IA (validador paliativo) na conciliacao.
--
-- Marca as conciliacoes que a IA ja avaliou (com ou sem match encontrado), para
-- que o "sweep" por IA nao chame o LLM de novo para o mesmo item (memoria/cache).
-- Default false para nao afetar linhas existentes.

ALTER TABLE reconciliations ADD COLUMN ia_tentada BOOLEAN NOT NULL DEFAULT FALSE;

-- Acelera a busca das pendencias que ainda faltam passar pela IA.
CREATE INDEX idx_reconciliations_ia_pendente
    ON reconciliations (empresa_id, status, camada, ia_tentada);
