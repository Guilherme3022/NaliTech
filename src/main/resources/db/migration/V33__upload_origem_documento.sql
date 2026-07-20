-- Papel do documento no upload: EXTRATO (lado banco) x SISTEMA (contas a pagar/
-- receber, caixa interno). Base para o matching extrato x sistema.
alter table uploads add column if not exists origem varchar(20);

-- Uploads antigos: assume EXTRATO por ser o caso mais comum de conciliacao.
update uploads set origem = 'EXTRATO' where origem is null;
