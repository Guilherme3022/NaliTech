-- Natureza de saldo da conta: DEVEDORA / CREDORA (o que o layout legado D-/C- indicava).
-- E o "saldo normal" da conta (Ativo/Despesa = devedora; Passivo/PL/Receita = credora).
-- NAO e o lado do lancamento (debito/credito da partida dobrada), que e por movimentacao.
ALTER TABLE chart_of_accounts ADD COLUMN natureza_saldo VARCHAR(10);
