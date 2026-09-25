# Ajustes de Conciliação — Rodada 2 (Back + Front)

> **Status: IMPLEMENTADO** (A, B, C, D, E, F). Migrations `V39`, `V40`, `V41`.
> Não compilado/rodado neste ambiente (sem JDK/Node) — rodar `mvn test` e
> `npm run typecheck && npm run build` antes de subir.

Repositórios: `NaliTech/` (Spring Boot) e `NaliTechFront/` (React + Vite + MUI).

---

## Contexto / arquivos de exemplo (na pasta pai dos projetos)

- **TXT correto:** `47 - Pagamentos - Pagamento de Fornecedores - 202607.txt`
- **Planilha de origem (contas a pagar, lado SISTEMA):** `pagamentos JULHO DE 2026.xls`

### Formato do TXT decodificado (partida dobrada)

```
10/07/2026;4921;;1362;;477,20;VLR REF PAGTO BCONTROL 1191 PARC 1;47
```

| Pos | Conteúdo | Origem |
|-----|----------|--------|
| 1 | `10/07/2026` | data (`dd/MM/yyyy`) |
| 2 | `4921` | **código da conta de DÉBITO** (`ChartOfAccount.codigo`) |
| 3 | *(vazio)* | reservado — deixar vazio |
| 4 | `1362` | **código da conta de CRÉDITO** |
| 5 | *(vazio)* | reservado — deixar vazio |
| 6 | `477,20` | **valor** — positivo, 2 casas, **vírgula** decimal, **sem sinal** |
| 7 | `VLR REF PAGTO ...` | **histórico** = descrição do movimento (MAIÚSCULO) |
| 8 | `47` | **código da empresa contábil** (campo novo, numérico, no cliente) |

> Substitui o formato anterior `;DC`. A direção é dada pelas duas contas.

---

## Decisões (fechadas com você)

1. `codigoEmpresaContabil` = **numérico** (inteiro), guardado como número. Um por cliente.
2. Campos 3 e 5 do TXT = **sempre vazios** (deixar como está).
3. Histórico = **descrição do movimento**. Caso "só extrato", mantém como já está.
4. `recebeSistema` = **por conciliação** (não fixo no cliente).
5. Item sem as duas contas no export = **exportar com o campo da conta vazio**.
6. Valor no TXT = **vírgula** decimal e **sem sinal**.

---

# ÉPICO A — Selecionar conta de DÉBITO e de CRÉDITO na conciliação

**Problema:** a tela de conciliação só tem um seletor. Para o TXT de partida dobrada é
preciso escolher **as duas** (débito e crédito). O modelo já suporta
(`Movement.contaDebitoId`/`contaCreditoId`, `ClassificationService.setEntry`, `DoubleEntryService`).

### Backend
- [ ] **A1** `ConfirmRequest` aceita `contaDebitoId` + `contaCreditoId` (mantém
  `contaSugerida` legado).
- [ ] **A2** `ReconciliationService.confirm(...)`: com as duas contas → `setEntry(...)`;
  com só uma → comportamento atual (`applyCounterpart`, banco automático).
- [ ] **A3** `BatchConfirmItem` ganha `contaDebitoId`/`contaCreditoId`.
- [ ] **A4** `ReconciliationResponse` expõe `contaDebitoId`/`contaCreditoId` do movimento +
  a sugestão para **os dois lados** (ver Épico E) e o **banco** resolvido para pré-preencher.
- [ ] **A5** Pré-preenchimento conforme direção (regra do `DoubleEntryService`):
  - **SAÍDA (valor negativo)** → débito = contrapartida (fornecedor/despesa), crédito = banco.
  - **ENTRADA (valor positivo)** → débito = banco, crédito = contrapartida (cliente/receita).

### Frontend
- [ ] **A6** `ReconciliationReview.tsx`: dois `AccountSelect` (**Conta débito** e **Conta
  crédito**), pré-preenchidos por A4/A5/Épico E.
- [ ] **A7** `confirm` e `confirm-batch` enviam os dois ids.
- [ ] **A8** Mostrar as duas contas no card do item.

---

# ÉPICO B — Editar movimentação NA tela de conciliação

### Frontend
- [ ] **B1** `MovementsPage.tsx`: **remover** os dois `AccountSelect` do form de edição
  (deixar data/valor/descrição/documento/tipo).
- [ ] **B2** `ReconciliationReview.tsx`: botão **"Editar movimentação"** por item → dialog
  de edição chamando `PUT /movements/{id}` (sem contas). Extrair o form da `MovementsPage`.
- [ ] **B3** Após salvar, invalidar pendências/summary.

### Backend
- [ ] **B4** `UpdateMovementRequest`: manter contas no contrato (não quebra), mas o front
  não envia mais.

---

# ÉPICO C — Conciliação **sem** "sistema" (só extrato)

### Backend
- [ ] **C1** `Conciliacao` ganha `recebeSistema BOOLEAN NOT NULL DEFAULT TRUE`
  (migration `V39__conciliacao_recebe_sistema.sql`).
- [ ] **C2** `CreateConciliacaoRequest` aceita `recebeSistema`; endpoint para alternar
  (`POST /conciliacoes/{id}/recebe-sistema?valor=false`).
- [ ] **C3** Quando `recebeSistema=false`: pular matching extrato×sistema; cada movimento
  do extrato vai direto para classificação (débito/crédito). Não marcar "sem correspondência".

### Frontend
- [ ] **C4** Switch **"Não envia planilha de contas a pagar/receber"** na tela de conciliação.
- [ ] **C5** Ligado: `ReconciliationSplitView` mostra **só o Extrato** (esconde card
  "Sistema"), esconde upload do sistema e o rótulo "sem correspondência".

---

# ÉPICO D — TXT partida dobrada + código da empresa contábil no cliente

### Backend
- [ ] **D1** `Client`: campo **numérico** `codigoEmpresaContabil` (INT) — migration
  `V40__client_codigo_empresa_contabil.sql`; exposto em `CreateClientRequest`,
  `UpdateClientRequest`, `ClientResponse`.
- [ ] **D2** `ConciliacaoExportService`: novo layout
  `data;contaDebito;;contaCredito;;valor;historico;codigoEmpresaContabil`:
  - contas = `ChartOfAccount.codigo` de `contaDebitoId`/`contaCreditoId` (resolver id→código em lote);
    **se faltar** a conta → campo **vazio**.
  - valor = positivo, 2 casas, **vírgula**.
  - histórico = descrição do movimento (MAIÚSCULO).
  - último campo = `codigoEmpresaContabil` do cliente.
- [ ] **D3** Remover o layout antigo `;DC` e ajustar/remover seus testes; criar testes do novo.

### Frontend
- [ ] **D4** `ClientFormDialog.tsx`: campo **"Código da empresa contábil"** (numérico).
- [ ] **D5** Botão "Gerar TXT" usa o novo formato; avisar se o cliente estiver sem o código.

---

# ÉPICO E — Memória de **partida dobrada** (aprender débito **e** crédito) 🆕

**Pedido:** a descrição geralmente casa com o **débito** (contrapartida), mas o que foi
preenchido no **crédito** também deve ser guardado, para que na próxima vez **as duas**
contas venham preenchidas. E quando o valor é **positivo** (entrada), a contrapartida vai
para o **crédito** — então guardamos os dois lados de qualquer forma.

### Backend
- [ ] **E1** `LearningHistory`: guardar **as duas contas** — renomear `conta_id` →
  `conta_debito_id` e adicionar `conta_credito_id` (migration
  `V41__learning_history_partida_dobrada.sql`). Mantém `descricao_padrao`, chave de CNPJ
  (`#digitos`), `ocorrencias`, escopo por cliente.
- [ ] **E2** `LearningService.recordDecision(...)`: passar a receber e gravar
  **contaDebitoId + contaCreditoId** (por descrição **e** por CNPJ), incrementando ocorrências.
- [ ] **E3** `LearningEventListener` / `ReconciliationService.confirm`: ao confirmar, enviar
  as **duas** contas escolhidas para o aprendizado.
- [ ] **E4** `AiSuggestionProvider.SuggestedAccount`: passar a carregar
  `contaDebitoId` + `contaCreditoId` (ambos podem ser nulos).
  - `HeuristicSuggestionProvider` (memória) → devolve **o par aprendido** (débito+crédito).
  - `ChartNameSuggestionProvider` (nome) → devolve a **contrapartida** no lado certo
    conforme entrada/saída; o outro lado (banco) é resolvido pelo `DoubleEntryService`.
- [ ] **E5** `AiSuggestion` (persistência) e `SugestaoView` (response) expõem os dois lados,
  para o front pré-preencher **os dois** seletores (liga com A4/A6).
- [ ] **E6** Testes: confirmar com (débito X, crédito Y) → próxima movimentação igual sugere
  X e Y; entrada (valor +) aprende contrapartida no crédito.

---

# ÉPICO F — Sugestão mais proativa (2+ palavras iguais/parecidas) 🆕

**Pedido:** se achou **mais de uma palavra igual ou muito parecida**, já pode oferecer
como sugestão.

### Backend
- [ ] **F1** Casamento por nome (memória e plano de contas): considerar **2+ tokens
  iguais OU muito parecidos** como sugestão válida. Hoje a interseção é por token **exato**;
  adicionar comparação **fuzzy por token** (prefixo/raiz ou distância pequena, ex.:
  `decker`≈`deckers`, `distribuidora`≈`distrib`).
- [ ] **F2** Aplicar em `StringSimilarity` (novo `commonOrSimilarTokenCount` / `tokenOverlapFuzzy`)
  e usar no `HeuristicSuggestionProvider` e no `ChartNameSuggestionProvider` (mantendo a
  guarda: 2+ tokens ou lado de token único).
- [ ] **F3** (opcional) Refletir o mesmo no score de nome do `MatchingService` (Processo A),
  para sugerir match quando 2+ palavras batem. Valor continua sendo gate de segurança.
- [ ] **F4** Testes: `black deckers` casa com `black decker`; 2 palavras parecidas geram sugestão.

---

## Migrations previstas

| Migration | Mudança |
|-----------|---------|
| `V39__conciliacao_recebe_sistema.sql` | `conciliacoes ADD COLUMN recebe_sistema BOOLEAN NOT NULL DEFAULT TRUE` |
| `V40__client_codigo_empresa_contabil.sql` | `clients ADD COLUMN codigo_empresa_contabil INTEGER` |
| `V41__learning_history_partida_dobrada.sql` | renomear `conta_id`→`conta_debito_id`, add `conta_credito_id` |

*(Numeração final conforme a última migration no momento da execução.)*

---

## Ordem de execução sugerida
1. **E** (memória de partida dobrada) + **A** (dois seletores) — andam juntos.
2. **D** (cliente + TXT novo).
3. **F** (sugestão mais proativa).
4. **B** (editar na conciliação / tirar contas da edição).
5. **C** (sem sistema).
