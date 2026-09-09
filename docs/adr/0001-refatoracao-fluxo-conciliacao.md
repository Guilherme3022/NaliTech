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
- [x] B15. **Atribuicao global otima** (aproximada) do match: passo `optimize` por
      cliente/competencia que refaz os itens PENDENTES gerando todos os pares validos,
      ordenando por nota e casando guloso-ordenado (cada lancamento usado uma vez no
      melhor par). Roda ao fim do pipeline e via endpoint `POST /reconciliations/optimize`.
- [x] B14. **Aprende vinculos manuais** (apelidos de contraparte): ao confirmar um
      match, guarda que o nome do extrato e o do sistema sao a mesma parte
      (`counterpart_aliases`, V35); o match automatico futuro usa isso como sinal forte.
- [x] B13. IA de classificacao **aprende por CNPJ/CPF** (chave exata, alem do nome):
      mesma contraparte reconhecida mesmo com variacao de grafia; sugestao por CNPJ
      tem confianca maior. Fallback por similaridade de nome mantido.
- [x] B12. **Conta bancaria por extrato** (migration V34): upload/movimento guardam
      `bank_account_id`; `DoubleEntryService` usa a conta do banco correto na partida
      dobrada (corrige clientes com varios bancos), com fallback para o banco padrao.
- [x] B11. IA: captura da **contraparte + CNPJ/CPF** da linha seguinte no extrato BB
      (antes perdida) -> match por nome e sugestao de conta muito melhores; bonus de
      score quando CNPJ bate nos dois lados; preferencia do **extrato como dirigente**
      do item (lado lancado na partida dobrada).
- [x] B10. Match **independente do papel do documento**: casa entre arquivos
      diferentes (uploadId distinto) do mesmo cliente mesmo sem marcar extrato/sistema
      (papel vira bonus no score). Modelo simetrico com item unico por par (dirigente +
      contrapartida), sem duplicar. Corrige o "zero match" quando os papeis nao eram marcados.
- [x] B9. IA de match turbinada: score valor+data+nome (0..1) com tolerancia de
      valor (2%) resgatada por nome forte, janela de 7 dias, e sugestao de conta
      **proativa** (regra+aprendizado, sem custo de LLM) ja pre-preenchida no item.
- [x] B8. Suporte ao layout **Banrisul** (dia isolado + cabecalho de mes;
      debito com sinal de menos no fim; contraparte na linha `NOME:`).
      Dispatcher no `LineMovementExtractor` + teste unitario cobrindo os dois
      formatos (Banrisul e generico BB/caixa/contas a pagar).

## Consequências

- Conciliação passa a fazer sentido contábil (banco x sistema).
- Menos itens duplicados; pendências reais para revisão manual.
- A tela de detalhe do lote consegue listar os itens por cliente+competência.
