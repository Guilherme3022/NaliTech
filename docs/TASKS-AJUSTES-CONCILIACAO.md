# Ajustes de Conciliação — Plano de Mini-Tasks (Back + Front)

> ## ✅ Status de implementação (atualizado)
>
> | Task | Backend | Frontend | Observações |
> |------|---------|----------|-------------|
> | 1 — Remover IA externa | ✅ feito | ✅ feito | Arquivos/endpoints/entidade/migration/`.env`/`application.yml` limpos. `IA-CONCILIACAO.md` removido. *Pendente (baixa prioridade):* limpar menções em `DEPLOY.md`/`MODULOS.md`/`README.md`. |
> | 2 — Plano por cliente | ✅ feito | ⚠️ parcial | Bug de escopo corrigido (`findLancaveisForCliente`) + novo `ChartNameSuggestionProvider`. *Pendente:* higiene de dados (2.4, exige contador), mostrar origem da conta no seletor (2.7), testes (2.5). |
> | 3 — Entrada/Saída | ✅ feito | ⚠️ parcial | `RawMovement.tipoIndicador`, `MovementNormalizer.resolverTipo` + **normalização de sinal por tipo**, extrator OCR robusto (**menos à frente** `-875,40` — bug principal —, parênteses, menos ao fim, D/C isolado/adjacente), OFX TRNTYPE, coluna D/C no CSV/Excel, `PUT /movements/{id}` aceita `tipo`. **Testes adicionados** (`LineMovementExtractorTest`, `MovementNormalizerTest`). *Pendente:* parser Sicredi dedicado com amostra OCR (3.4), badge Entrada/Saída na UI (3.8). |
> | 4 — Não conciliar (dispensar) | ✅ feito | ✅ feito | Status `DISPENSADO`/`IGNORADO`, endpoints `dispensar`/`dispensar-batch`, botões por item e em lote. Sem migration (status é VARCHAR sem CHECK). |
> | 5 — Gerar TXT a qualquer momento | ✅ feito | ✅ feito | Novo `GET /reconciliations/export` (não exige concluída), layout `data;codigo;HISTORICO;valor;DC` parametrizável, botão "Gerar TXT". *Pendente:* modal de opções avançadas (5.8), confirmar layout do sistema contábil de destino, testes (5.5). |
>
> **Não compilado/rodado neste ambiente** (sem JDK/Node disponível) — revisar com
> `mvn test` e `npm run typecheck && npm run build` antes de subir.

---


> Documento de execução para 5 frentes solicitadas:
> 1. **Remover totalmente a IA externa** (tela + backend)
> 2. **Plano de contas por cliente** (parou de misturar entre clientes da mesma empresa)
> 3. **Identificar entrada x saída** (hoje entra tudo como ENTRADA)
> 4. **Botão "não conciliar / dispensar"** item no meio da conciliação
> 5. **Gerar o TXT a qualquer momento** + **corrigir o formato do TXT**
>
> Repositórios:
> - Back: `NaliTech/` (Spring Boot, Java 17)
> - Front: `NaliTechFront/` (React + Vite + MUI + React Query)

---

## Diagnóstico rápido (causa raiz de cada problema)

| # | Sintoma | Causa raiz encontrada | Arquivo(s) |
|---|---------|----------------------|-----------|
| 2 | Plano de contas se mistura entre clientes; não acha "Black and Decker" | A sugestão de conta busca **todas as contas da empresa** ignorando o cliente. Além disso, a heurística só casa contra o **histórico de aprendizado** (classificações passadas), **não contra os nomes das contas do plano** — cliente novo nunca recebe sugestão, mesmo tendo a conta cadastrada. | `ClassificationSuggestionService.java:81` (`findLancaveisByEmpresa`), `HeuristicSuggestionProvider.java` |
| 3 | Entra tudo como ENTRADA | `MovementNormalizer` define o tipo **só pelo sinal do valor** (`signum() < 0 ? SAIDA : ENTRADA`). O extrato Sicredi de exemplo é **PDF escaneado (imagem) → OCR**, e o extrator (`MONEY_CD`) exige o `D`/`C` colado ao valor (`\s*[CD]\b`). No OCR o indicador D/C fica distante/perdido, então cai no fallback `MONEY_ANY` (sem sinal) → positivo → ENTRADA. | `MovementNormalizer.java`, `LineMovementExtractor.java`, `PdfParser.java` |
| 5 | TXT "estranho" / não bate com o exemplo | O código atual gera **TSV (separado por TAB)** com colunas `data/valor/descricao/conta_debito/conta_credito` — **não** o `data;codigo;descricao;valor;D/C` que você colou. E o export **exige conciliação CONCLUÍDA** (`requireConcluida`), então não dá pra gerar a qualquer momento. | `ConciliacaoExportService.java`, `ConciliacaoController.java` |
| 4 | Falta botão "não precisa conciliar" | Só existe `reject`, que devolve a movimentação para o pool (`NORMALIZADO`) e ela **volta a aparecer**. Não há status "dispensado/ignorado". | `ReconciliationService.reject`, `MovementStatus`, `ReconciliationStatus` |
| 1 | IA externa precisa sair | Espalhada em vários pontos (ver Task 1). | vários |

---

## Sobre o formato do TXT — **formato CONFIRMADO e consistente**

Formato alvo (confirmado pelo cliente; `D` = **devedora**, `C` = **credora**, sob a
ótica da conta banco):

```
data;codigo;HISTORICO;valor;DC
01/09/2026;1001;SALDO ANTERIOR;15000.00;D
01/09/2026;1002;PIX RECEBIDO CLIENTE ALFA;1250.00;D
01/09/2026;1003;PAGAMENTO FORNECEDOR XPTO;-875.40;C
02/09/2026;1011;TARIFA BANCARIA;-45.00;C
```

Regra de natureza (coerente — sinal e D/C andam juntos):

| Movimento | Sinal do valor | D/C | MovementType |
|-----------|----------------|-----|--------------|
| Dinheiro **entra** no banco (recebimento, PIX recebido, resgate, estorno a favor) | **positivo** | **D** (devedora) | **ENTRADA** |
| Dinheiro **sai** do banco (pagamento, tarifa, PIX enviado, aplicação) | **negativo** | **C** (credora) | **SAÍDA** |

Especificação do arquivo:

- Separador de campos: `;`
- `data` = `dd/MM/yyyy`
- `codigo` = número **sequencial** do lançamento (1001, 1002, …). *(A confirmar: se o
  destino exigir o código da conta do plano, trocamos por `ChartOfAccount.codigo`. Deixar
  parametrizável.)*
- `HISTORICO` = descrição em MAIÚSCULAS, saneada (`trim`, sem `;`, sem quebra de linha)
- `valor` = **com sinal** (`+` entrada / `−` saída), **2 casas**, separador decimal
  **ponto** neste layout *(parametrizável: vírgula p/ Domínio/Alterdata, etc.)*
- `DC` = `D` (entrada/devedora) ou `C` (saída/credora)
- `SALDO ANTERIOR`: mantido conforme o exemplo, mas **configurável** (flag
  `incluirSaldoAnterior`) — muitos sistemas contábeis ignoram/rejeitam saldo de abertura.

Correções necessárias no gerador atual (`ConciliacaoExportService`):

1. Hoje gera **TSV (TAB)** com colunas `data/valor/descricao/conta_debito/conta_credito`
   e exporta **UUIDs** de conta — precisa virar o layout acima.
2. `trim()` em todos os campos (o exemplo antigo tinha `; -45.00` com espaço).
3. Derivar `DC` do `MovementType` (D=ENTRADA, C=SAÍDA) — agora que a Task 3 detecta o tipo.
4. Deixar separador de campo, separador decimal, encoding e flags parametrizáveis.

> ⚠️ **A confirmar com o contador:** o **sistema contábil de destino** (Domínio,
> Alterdata, Fortes, Contmatic…) para travar separador decimal, encoding e se `codigo`
> é sequencial ou código de conta. O gerador fica **parametrizável** para acomodar.

---

# TASK 1 — Remover totalmente a IA externa (back + front)

**Objetivo:** tirar de tela e do backend toda a camada de IA externa (LLM/sweep),
mantendo a conciliação algorítmica e a classificação determinística funcionando.

### Backend

- [ ] **1.1** Excluir a IA de conciliação (sweep):
  - Remover `modules/reconciliation/service/ReconciliationAiSweepService.java`,
    `ReconciliationAiItemProcessor.java` e `modules/reconciliation/ai/AiReconciliationMatcher.java`.
  - Remover do `ReconciliationController` os endpoints `POST /reconciliations/ai-sweep`,
    `GET /reconciliations/ai-sweep/{jobId}`, `GET /reconciliations/ai-sweep` e o DTO `AiSweepJob`.
  - Remover o campo `iaTentada`/coluna `ia_tentada` de `Reconciliation` (nova migration
    `V38__remove_reconciliation_ia.sql` com `ALTER TABLE reconciliation DROP COLUMN ia_tentada`).
- [ ] **1.2** Excluir a IA de classificação de conta (LLM):
  - Remover `modules/account/ai/LlmSuggestionProvider.java`.
  - Simplificar `SuggestionProviderSelector` para retornar **apenas** o provedor
    heurístico/determinístico (remover a lógica `AI_PROVIDER=IA`, o `@Value("${AI_PROVIDER}")`
    e o `llm.isConfigured()`). `providers()` e `deterministicProviders()` passam a ser iguais.
- [ ] **1.3** Excluir o módulo de medição de IA (faturamento):
  - Remover o pacote `modules/aiusage/` inteiro (controller, service, dto, entity, repository).
  - Migration `V39__drop_ai_usage.sql` com `DROP TABLE ai_usage_monthly`.
  - Remover métricas Prometheus `nalitech.ai.calls` / `nalitech.ai.tokens` (procurar por
    `MeterRegistry`/`ai.calls` no observability).
- [ ] **1.4** Limpar configuração:
  - Em `application.yml`, remover `reconciliation.ai.*` (`ai-min-confidence`,
    `ai-max-candidates`, `ai-inline`) e qualquer bloco `AI_*`.
  - Remover das `.env*` as chaves `RECONCILIATION_AI_ENABLED`, `AI_API_URL`, `AI_API_KEY`,
    `AI_MODEL`, `AI_PROVIDER`, `RECON_AI_*`.
- [ ] **1.5** Documentação: apagar `IA-CONCILIACAO.md`; remover a seção 5 (IA de
  classificação) do `DEPLOY.md` e as menções à IA em `MODULOS.md`/`README.md`.
- [ ] **1.6** Garantir que compila e testes passam: `mvn -q compile` e `mvn -q test`.
  Ajustar imports/usos órfãos (ex.: `ReconciliationPipelineListener`, eventos `SweepIaConcluido`).

### Frontend

- [ ] **1.7** Remover `modules/reconciliation/components/AiSweepPopup.tsx` e sua montagem
  em `shared/components/AppLayout.tsx` (import + `<AiSweepPopup />` e o comentário do popup).
- [ ] **1.8** Em `modules/reconciliation/components/ReconciliationReview.tsx`: remover o
  botão **"Validar … com IA"** (`AutoAwesomeIcon`, `useStartAiSweepMutation`, `aiSweep`,
  e o `disabled={... semMatch}` associado).
- [ ] **1.9** Em `modules/reconciliation/api.ts`: remover `startAiSweep`, `aiSweepStatus`,
  `aiSweepActive`. Em `hooks.ts`: remover `useStartAiSweepMutation`, `useAiSweepActiveQuery`.
  Em `types.ts`: remover o type `AiSweepJob`.
- [ ] **1.10** Procurar telas de "consumo de IA"/faturamento por IA (settings/finance) e
  remover. `grep -rin "sweep\|ai-usage\|aiUsage\|IA " src` para varrer resíduos.
- [ ] **1.11** `npm run typecheck` e `npm run build` limpos.

---

# TASK 2 — Plano de contas específico por cliente

**Objetivo:** cada cliente enxerga/consome **apenas** o seu plano (contas específicas do
cliente + as compartilhadas do escritório com `cliente_id NULL`), sem vazar contas de
outros clientes. E o sistema volta a "achar" contas como *Black and Decker*.

### Backend

- [ ] **2.1** **Bug principal:** em `ClassificationSuggestionService.suggest(...)`,
  trocar `chartRepository.findLancaveisByEmpresa(empresaId)` por
  `chartRepository.findLancaveisForCliente(empresaId, movement.getClienteId())`.
  (Se `clienteId` for nulo, cair no comportamento antigo só com as compartilhadas.)
- [ ] **2.2** **Novo provedor determinístico de nome de conta** (`ChartNameSuggestionProvider`):
  hoje a heurística só casa contra `LearningHistory` (classificações passadas). Cliente
  novo nunca recebe sugestão. Criar um provedor que casa a **descrição da movimentação**
  contra o **nome das contas do plano do cliente** (`ChartOfAccount.nome`) usando
  `DescriptionNormalizer` + `StringSimilarity.tokenSimilarity`/`ratio`. Assim "deposito
  black and decker" → conta "BLACK AND DECKER". Adicionar depois do histórico e antes do
  fallback `NENHUMA` no `SuggestionProviderSelector.deterministicProviders()`.
- [ ] **2.3** Auditar **todos** os pontos que listam contas para conciliação/sugestão e
  garantir escopo por cliente. `findLancaveisByEmpresa` deve ser usado **só** para
  operações administrativas globais (nunca no fluxo de sugestão/seleção de conta). Conferir
  `AccountService.listLancaveis` (já usa `findLancaveisForCliente` ✅) e `LayoutExportService`.
- [ ] **2.4** **Higiene dos dados existentes** (contas que já "se misturaram"): script/endpoint
  admin para revisar contas com `cliente_id NULL` que deveriam ser específicas. Documentar
  em `docs/`. (Não migrar automático sem revisão do contador — risco de reassociar errado.)
- [ ] **2.5** Teste: dois clientes na mesma empresa, cada um com conta de mesmo nome/código;
  sugestão para movimentação do cliente A **nunca** retorna conta do cliente B.

### Frontend

- [ ] **2.6** Garantir que o seletor de conta na conciliação e a tela de plano de contas
  sempre enviam `clienteId` (checar `modules/accounts/hooks.ts` e o `ClientCompetenceSwitcher`).
- [ ] **2.7** Exibir no seletor de conta a origem (`Cliente` x `Compartilhada do escritório`)
  para o contador diferenciar, evitando recadastro duplicado.

---

# TASK 3 — Identificar entrada x saída corretamente

**Objetivo:** débitos (saídas) e créditos (entradas) classificados certo, incluindo em
extratos **escaneados via OCR** (caso Sicredi de exemplo).

### Backend

- [ ] **3.1** Adicionar campo explícito de natureza no `RawMovement`
  (`String tipoIndicador` = `"C"`/`"D"`/`null`), para o parser transmitir a natureza
  **sem depender só do sinal** do valor.
- [ ] **3.2** Em `MovementNormalizer.toMovement`, derivar `MovementType` nesta ordem:
  1. se `raw.tipoIndicador()` presente → `D` = SAIDA, `C` = ENTRADA;
  2. senão, pelo sinal do valor (`signum() < 0` → SAIDA);
  3. registrar em log `WARN` quando não der pra determinar (hoje é silencioso).
- [ ] **3.3** Robustecer `LineMovementExtractor.extractGeneric` para OCR:
  - Aceitar `D`/`C` **em qualquer posição** da linha (não só colado ao valor):
    ex.: procurar token isolado `\b[DC]\b` além do `MONEY_CD`.
  - Suportar **débito entre parênteses** `(1.200,50)` e **sinal de menos no fim** `1.200,50-`.
  - Suportar layout de **duas colunas** (uma p/ débito, outra p/ crédito): quando houver
    dois valores monetários na linha, o preenchido indica a natureza.
  - Preencher `tipoIndicador` no `RawMovement` retornado.
- [ ] **3.4** **Layout Sicredi dedicado** (como já existe o Banrisul): criar
  `extractSicredi(...)` acionado por `text.contains("SICREDI")`. Mapear a coluna de
  natureza/valor do extrato Sicredi (validar com o PDF de exemplo em
  `../<arquivo> sicredi julho26.pdf`). Enquanto não confirmado o layout, o 3.3 já reduz o
  problema.
- [ ] **3.5** Conferir a qualidade do **OCR** (`modules/ocr/`): se o D/C some no OCR, avaliar
  `-layout`/densidade. Sem D/C no texto, nenhum parser resolve.
- [ ] **3.6** Testes com amostras reais (Sicredi + genérico): linhas de débito → SAIDA,
  crédito → ENTRADA; linha "SALDO" ignorada.

### Frontend

- [ ] **3.7** Na revisão da conciliação e na tela de movimentações, exibir badge de
  **Entrada/Saída** (verde/vermelho) e permitir **corrigir o tipo manualmente** (útil quando
  o OCR erra). Endpoint `PATCH /movements/{id}` com `{ tipo }`.
- [ ] **3.8** Mostrar totalizadores separados de Entradas x Saídas no resumo da competência.

---

# TASK 4 — Botão "não precisa conciliar" (dispensar/excluir dinamicamente)

**Objetivo:** no meio da conciliação, dispensar um item que não precisa ser conciliado; ele
**sai da lista** e **não volta** (diferente de `reject`, que hoje devolve ao pool).

### Backend

- [ ] **4.1** Novo status: adicionar `DISPENSADO` em `ReconciliationStatus` e `IGNORADO` (ou
  reutilizar) em `MovementStatus`. Migration `V40__reconciliation_dispensado.sql` se houver
  constraint/enum no banco.
- [ ] **4.2** Novo método `ReconciliationService.dispensar(UUID id, String motivo)`:
  marca `Reconciliation` como `DISPENSADO`, seta a movimentação para um status terminal
  (`IGNORADO`) que **não reentra** na conciliação/reprocess, grava `motivo` e auditoria.
- [ ] **4.3** Endpoint `POST /reconciliations/{id}/dispensar` (body opcional `{ motivo }`) e
  `POST /reconciliations/dispensar-batch` (lista de ids).
- [ ] **4.4** Garantir que itens `DISPENSADO`/`IGNORADO` sejam **excluídos** de:
  `pending`, `reprocessPending`, `optimize`, `summary` (ou somados numa linha "Dispensados")
  e do **export TXT** (Task 5).
- [ ] **4.5** Teste: dispensar um item → some de `pending`, não volta em `reprocess`, não
  entra no TXT.

### Frontend

- [ ] **4.6** Em `ReconciliationReview.tsx`: botão **"Não conciliar"** por linha e ação em
  lote (usando os itens selecionados). Ícone `BlockIcon`/`DoDisturbIcon`. Confirmar com
  diálogo e `motivo` opcional.
- [ ] **4.7** `api.ts`: `dispensar(id, motivo)` e `dispensarBatch(ids, motivo)`. `hooks.ts`:
  `useDispensarMutation` / `useDispensarBatchMutation` invalidando `pending` e `summary`.
- [ ] **4.8** Atualização otimista: a linha some da lista imediatamente. Mostrar contador de
  "Dispensados" no resumo, com opção de ver/reverter.

---

# TASK 5 — Gerar o TXT a qualquer momento + corrigir o formato

**Objetivo:** botão que gera o TXT **quando o usuário quiser** (não só com conciliação
concluída), no **formato correto** para o sistema contábil de destino.

### Backend

- [ ] **5.1** **Não exigir CONCLUÍDA:** novo caminho de export que aceita
  `clienteId + competencia` (sem depender de uma `Conciliacao` concluída). Manter o
  `requireConcluida` só para o "arquivo oficial" de fechamento, se desejado.
  - Endpoint: `GET /reconciliations/export?clienteId=&competencia=YYYY-MM&formato=TXT&somente=CONCILIADOS`.
- [ ] **5.2** **Corrigir o layout** em `ConciliacaoExportService` (ver seção "Sobre o
  formato do TXT"):
  - Colunas: `data;codigoConta;historico;valor;naturezaDC`.
  - `codigoConta` = `ChartOfAccount.codigo` **do cliente** (resolver o UUID →
    código; hoje exporta UUID ❌).
  - `valor` **com sinal** (+entrada/−saída), 2 casas, separador decimal **configurável**
    (ponto neste layout; vírgula opcional).
  - `naturezaDC` derivada do `MovementType`: **D=ENTRADA (devedora)**, **C=SAÍDA (credora)**.
  - `trim()` em todos os campos; remover `;`/quebras de linha do histórico.
  - **Filtrar** linhas de saldo/abertura e itens `DISPENSADO`.
  - Separador `;` para TXT (não TAB). Encoding: avaliar `Windows-1252` (muitos sistemas
    contábeis BR não leem UTF-8) — tornar configurável.
- [ ] **5.3** Parametrizar o layout de export (record `ExportLayout`: separador de campo,
  separador decimal, encoding, cabeçalho sim/não, filtro de status) para acomodar o
  sistema contábil de destino a confirmar com o contador.
- [ ] **5.4** Aproveitar `LayoutExportService`/`ImportLayout` já existentes se cobrirem o
  caso (evitar duplicar lógica de layout).
- [ ] **5.5** Testes: valida cabeçalho/linhas, decimais com vírgula, sem saldo/dispensados,
  código de conta correto por cliente.

### Frontend

- [ ] **5.6** Botão **"Gerar TXT"** na tela de conciliação (topo), habilitado a qualquer
  momento (não só concluída). Usa `clienteId + competencia` atuais do `ClientCompetenceSwitcher`.
- [ ] **5.7** `api.ts`: `export({ clienteId, competencia, formato, somente })` com
  `responseType: 'blob'` + download com nome `conciliacao-<cliente>-<competencia>.txt`.
- [ ] **5.8** Opções de geração (modal reaproveitando `ExportConfigModal`): formato (TXT/CSV),
  "somente conciliados" x "tudo", separador decimal, incluir cabeçalho. Persistir a última
  escolha por usuário.
- [ ] **5.9** Feedback de "gerado com sucesso" e aviso se houver pendências não conciliadas
  no período (o usuário decide gerar mesmo assim).

---

## Ordem sugerida de execução

1. **Task 1** (remover IA) — reduz superfície e evita mexer em código que vai sair.
2. **Task 2** (plano por cliente) — desbloqueia as sugestões corretas.
3. **Task 3** (entrada/saída) — corrige a natureza dos lançamentos.
4. **Task 4** (dispensar) — melhora o fluxo de revisão.
5. **Task 5** (TXT) — depende de 2/3/4 estarem certos para o arquivo sair correto.

## Definição de pronto (global)

- [ ] `mvn -q test` verde no back; `npm run typecheck && npm run build` verde no front.
- [ ] Sem referências residuais a IA (`grep -rin "ai-sweep\|LlmSuggestion\|aiusage\|AI_PROVIDER"`).
- [ ] Fluxo E2E: upload extrato Sicredi → entradas/saídas corretas → sugestões da conta do
      cliente → dispensar 1 item → gerar TXT no formato acordado, sem o item dispensado.
- [ ] Layout do TXT validado importando no sistema contábil de destino real.
