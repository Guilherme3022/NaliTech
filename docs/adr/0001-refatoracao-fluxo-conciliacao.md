# ADR 0001 — Refatoração do fluxo de conciliação (backend)

Status: concluído (aguardando teste em produção)
Data: 2026-07-20

## Contexto

O fluxo de conciliação estava, na prática, inutilizável:

1. O extrator de movimentações (`LineMovementExtractor`) lia mal os documentos
   reais (extrato BB, contas a pagar, gerenciador de caixa): perdia o sinal
   C/D, capturava número de documento como valor, pegava o saldo no lugar da
   transação e quebrava a descrição.
2. O matching (`MatchingService`) casava uma movimentação com *qualquer outra*
   da mesma empresa com mesma data+valor, **sem distinguir lado banco (extrato)
   do lado sistema (contas a pagar/receber)**. `origem` guardava só a extensão
   do arquivo (`"pdf"`), então não existia o conceito de "papel do documento".
3. Cada movimentação virava uma `Reconciliation`, gerando duplicatas que
   apontavam umas para as outras.
4. A tela de detalhe da conciliação (front) não mostrava match nenhum.

## Decisão

- Introduzir o conceito de **papel do documento** no upload: `EXTRATO` (lado
  banco) vs `SISTEMA` (lado contábil: contas a pagar/receber, caixa interno).
- Matching passa a ser **extrato ⇄ sistema**: cada item de conciliação é dirigido
  por uma linha de **extrato**, casada com a melhor movimentação do **sistema**
  (mesmo valor com sinal, data dentro de uma janela, descrição semelhante).
- Movimentações do sistema **não** geram item próprio; elas preenchem itens de
  extrato pendentes (bidirecional, independente da ordem de upload).
- Vínculo item ⇄ lote (`Conciliacao`) é feito por **cliente + competência**
  (ambos já carregam esses campos), sem precius popular `conciliacao_id`.

## Tarefas

- [x] B1. Reescrever `LineMovementExtractor` (sinal C/D, valor correto, saldo
      ignorado, descrição limpa, data de pagamento em contas a pagar).
- [x] B2. `OrigemDocumento {EXTRATO, SISTEMA}` + coluna `uploads.origem`
      (migration V33) + propagação upload → evento → `Movement.origem`.
- [x] B3. `MatchingService` extrato⇄sistema com janela de data, valor com sinal
      e similaridade; sistema preenche pendências de extrato (bidirecional).
- [x] B4. Query de candidatos do sistema em `MovementRepository`.
- [x] B5. Não criar `Reconciliation` para movimentação de origem `SISTEMA`.
- [x] B6. Expor `origem` no `UploadResponse` (para o front escolher/exibir).
- [x] B7. Melhorar a IA de sugestão de conta usando a contraparte
      (CNPJ/nome) extraída da descrição — normalização de histórico.
- [x] B8. Suporte ao layout **Banrisul** (dia isolado + cabecalho de mes;
      debito com sinal de menos no fim; contraparte na linha `NOME:`).
      Dispatcher no `LineMovementExtractor` + teste unitario cobrindo os dois
      formatos (Banrisul e generico BB/caixa/contas a pagar).

## Consequências

- Conciliação passa a fazer sentido contábil (banco x sistema).
- Menos itens duplicados; pendências reais para revisão manual.
- A tela de detalhe do lote consegue listar os itens por cliente+competência.
