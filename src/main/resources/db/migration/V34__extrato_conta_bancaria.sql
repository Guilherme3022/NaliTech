-- Conta bancaria (banco) a que um extrato pertence. Permite que clientes com varios
-- bancos (ex.: BB, Banrisul, Caixa) tenham o lancamento de partida dobrada feito contra
-- a conta contabil do banco correto, e nao contra um unico "banco padrao".
alter table uploads add column if not exists bank_account_id uuid;
alter table movements add column if not exists bank_account_id uuid;
