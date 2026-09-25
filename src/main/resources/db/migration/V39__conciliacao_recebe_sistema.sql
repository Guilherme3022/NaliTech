-- Nem toda conciliacao recebe a planilha do "sistema" (contas a pagar/receber).
-- Quando FALSE, a tela esconde o lado do sistema e o matching extrato x sistema nao roda:
-- cada lancamento do extrato vai direto para a classificacao (debito/credito).
ALTER TABLE conciliacoes ADD COLUMN recebe_sistema BOOLEAN NOT NULL DEFAULT TRUE;
