# Módulos do Nalitech — Guia de Uso

Este documento explica, de forma prática, **o que cada módulo faz** e **como
usar** o sistema no dia a dia. O foco é o fluxo principal: subir extratos →
processar → **conciliar** → classificar → exportar para o sistema contábil.

> Todas as rotas REST citadas são relativas à base da API (ex.: `/api`). O
> acesso é multi-empresa: cada requisição opera sobre a empresa do usuário
> autenticado (isolamento por `empresaId`).

---

## Visão geral do fluxo

```
Upload de extrato ──► Parser/Normalização ──► Conciliação ──► Classificação ──► Exportação
   (Uploads)            (Movimentações)        (Conciliação)   (Plano de contas)   (Exportação)
```

1. O contador **sobe um arquivo** (extrato bancário / OFX / CSV / PDF).
2. O sistema **valida, processa e normaliza** as movimentações.
3. Cada movimentação passa pela **conciliação automática** (ver seção dedicada).
4. O que não bate automaticamente vai para **revisão manual**.
5. Movimentações conciliadas são **classificadas** no plano de contas.
6. Os lançamentos são **exportados** no layout do sistema contábil do escritório.

---

## Perfis de acesso (papéis)

| Papel | O que enxerga |
|---|---|
| `ADMIN` | Tudo, incluindo Usuários, Auditoria, Empresa e Configurações |
| `CONTADOR` | Operação completa + Exportação, Empresa, Configurações |
| `AUXILIAR` | Operação (clientes, uploads, conciliação, plano de contas, fiscal) |
| `CLIENTE` | Apenas o **Portal do cliente** (`/portal`) |

---

## Módulos e como usar

### 1. Clientes — `/clients`
Cadastro das empresas atendidas pelo escritório e seus documentos.
- Listar/buscar: `GET /clients?search=...`
- Detalhe: `GET /clients/{id}` · Documentos: `GET /clients/{id}/documents`
- Criar/editar/excluir: `POST /clients`, `PUT /clients/{id}`, `DELETE /clients/{id}`

**Uso:** cadastre o cliente antes de subir extratos dele. É a entidade que
amarra uploads, conciliações e obrigações fiscais.

### 2. Uploads (extratos) — `/uploads`
Entrada de arquivos para processamento.
- Enviar arquivo: `POST /uploads` (multipart/form-data, campo `file`)
- Listar/acompanhar: `GET /uploads` · Detalhe: `GET /uploads/{id}`
- Excluir: `DELETE /uploads/{id}`

**Status do upload:** `RECEBIDO → VALIDANDO → PROCESSANDO → CONCLUIDO` (ou
`ERRO`). Acompanhe o `etapaAtual` para ver onde o processamento está.

**Uso:** suba o extrato; ao concluir o processamento, as movimentações são
geradas e a conciliação dispara automaticamente.

### 3. Movimentações — `/movements`
As linhas normalizadas extraídas dos extratos (data, valor, descrição).
- Status: `NORMALIZADO → CONCILIACAO_PENDENTE → CONCILIADO → CLASSIFICADO`.
- São geradas pelo pipeline de upload; normalmente você não as cria à mão.

### 4. Conciliação — `/reconciliations`
Coração do sistema. Veja a seção **"Como funciona a conciliação"** abaixo.
- Pendências: `GET /reconciliations/pending`
- Histórico: `GET /reconciliations/history?status=...`
- Confirmar: `POST /reconciliations/{id}/confirm` (opcional: conta sugerida)
- Rejeitar: `POST /reconciliations/{id}/reject`

### 5. Plano de contas — `/chart-of-accounts` e regras — `/account-rules`
Estrutura contábil e regras de classificação automática.
- Contas: `GET/POST/PUT/DELETE /chart-of-accounts`
- Regras: `GET/POST/PUT/DELETE /account-rules`

**Uso:** cadastre as contas e crie regras (ex.: "descrição contém TARIFA → conta
X") para classificar movimentações conciliadas automaticamente.

**Critérios da regra (Increment 6):** além de descrição e valor, a regra pode
filtrar por **tipo** (entrada/saída), **banco** e **documento/código** — todos
opcionais e combinados (E). Uma regra também define conta, centro de custo e filial.

**Escopo por cliente (Increment 3):** contas, regras e contas bancárias podem ser
**compartilhadas (escritório)** ou **específicas de um cliente**. Na classificação,
o que é específico do cliente **tem prioridade** sobre o compartilhado (fallback).
O aprendizado (`learning_history`) também é por cliente. Cada movimentação herda o
`clienteId` do upload.

### 5.1. Parametrização "De/Para" — `/parametrization`
A camada que **traduz** o histórico do extrato → conta contábil, ficando cada vez
mais automática conforme você parametriza.
- Fila do que **falta mapear**: `GET /parametrization/requests` (padrões
  conciliados ainda sem conta, agrupados por descrição + contagem + valor total).
- **Aplicar em lote**: `POST /parametrization/apply` com
  `{ descricaoContains, contaId, criarRegra }` — classifica todas as pendentes que
  casam com o termo e, se `criarRegra=true`, cria uma regra permanente (De/Para).

**Classificação automática:** ao conciliar uma movimentação, o sistema tenta
aplicar a conta sozinho (`AutoClassificationListener`) usando a conta sugerida na
conciliação ou o motor de sugestão (regra + histórico). Só aplica quando a
**confiança ≥ 90**; o resto cai na fila de parametrização para revisão humana.
Quanto mais o contador parametriza, mais o sistema classifica sozinho depois.

**IA plugável (Increment 9):** a sugestão de conta usa um provedor selecionável
(`AI_PROVIDER`): **HEURISTICA** (histórico, grátis — padrão) ou **IA** (LLM real,
compatível com a API da OpenAI/Groq/Ollama). A ordem é regra → IA → histórico →
manual, com fallback automático. Custos e configuração: ver `DEPLOY.md` seção 5.

### 5.6. Layouts de importação — `/import-layouts` (Increment 9)
Builder que mapeia as **colunas** de um CSV/planilha de origem para os campos do
sistema (data, valor, descrição, documento), por cliente.
- CRUD `/import-layouts`.
- **Preview ao vivo**: `POST /import-layouts/preview` aplica o mapeamento a um CSV
  colado e devolve as linhas mapeadas (para testar antes de salvar).

### 5.2. Partida dobrada (débito/crédito) — `/bank-accounts`
Cada movimentação vira um **lançamento contábil de dupla entrada**. A conta do
De/Para é a **contrapartida**; o outro lado é a **conta bancária**.
- Convenção: **ENTRADA** → Débito = Banco, Crédito = Contrapartida; **SAÍDA** →
  Débito = Contrapartida, Crédito = Banco (sem tipo, usa o sinal do valor).
- Contas bancárias: `GET/POST/PUT/DELETE /bank-accounts` (várias por empresa; a
  marcada como `padrao` é usada por default). Cada uma liga um banco a uma conta
  do plano de contas.
- Ajuste manual do lançamento: `POST /movements/{id}/entry` com
  `{ contaDebitoId, contaCreditoId }` — quando não for o banco padrão.
- O **arquivo contábil exportado** agora inclui as colunas **débito/crédito**
  (código do plano de contas) nos 5 sistemas.

### 5.3. Centro de custo — `/cost-centers` (Increment 4)
Apropriação de lançamentos a centros de custo (ex.: Comercial, Tecnologia).
- CRUD `/cost-centers` (escopo por cliente, igual às contas/regras).
- **Automático**: uma regra De/Para pode definir um centro de custo — ao
  classificar, se a regra casar, o centro de custo é aplicado sozinho.
- **Manual**: `POST /movements/{id}/cost-center` `{ centroCustoId }`.
- O export ganhou a coluna **centro de custo** (código) nos 5 sistemas.

### 5.4. Filiais (matriz/filial) — `/branches` (Increment 5)
Separação de lançamentos por filial (cada filial tem CNPJ próprio).
- CRUD `/branches` (escopo por cliente).
- **Automático**: uma regra De/Para pode definir a filial (aplicada ao classificar).
- **Manual**: `POST /movements/{id}/branch` `{ filialId }`.
- **Export por filial**: `POST /layouts/{sistema}/export?filialId=...` gera o arquivo
  **individual** daquela filial; sem `filialId` gera o **consolidado**. Os 5
  exporters ganharam a coluna **filial** (código).

### 5.5. Financiamentos/empréstimos — `/loan-contracts` (Increment 7)
Cadastro de contratos com contas de **principal, juros e encargos** + classificação
**curto/longo prazo** (escopo por cliente).
- CRUD `/loan-contracts`.
- **Vincular lançamento ao contrato**: `POST /movements/{id}/loan-contract`
  `{ loanContractId }` — já classifica o lançamento na conta de **principal**
  (ajuste manual de débito/crédito se for juros/encargo).
- *Refinamento futuro*: split automático principal-vs-juros por parcela (motor de
  amortização) — ainda não implementado.

### 6. Exportação (layouts) — `/layouts`
Gera o arquivo no formato do sistema contábil de destino.
- Sistemas suportados: `GET /layouts`
- Exportar: `POST /layouts/{sistema}/export` (opcional `?filialId=` para arquivo individual)
- **Validação pré-export** (Increment 6): `GET /layouts/validation?inicio&fim[&filialId]` —
  lista lançamentos incompletos (sem débito/crédito) antes de gerar o arquivo.
- Histórico: `GET /layouts/exports/history`

**Uso:** após conciliar e classificar, exporte para o layout do sistema que o
escritório usa. Acesso: `ADMIN`/`CONTADOR`.

### 7. Agenda fiscal — `/fiscal-obligations`
Controle de obrigações e vencimentos.
- Listar: `GET /fiscal-obligations` · Próximas: `GET /fiscal-obligations/upcoming?dias=7`
- Criar/editar/excluir: `POST/PUT/DELETE /fiscal-obligations`

**Uso:** cadastre obrigações (com vencimento) para receber alertas de "vence em
breve"/"vencido".

### 8. Financeiro do escritório — `/office`
Mensalidades/cobranças dos clientes do escritório. **Roda em modo simulado por
padrão.** Para ligar cobrança real (Asaas, PIX, boleto, webhooks), veja
**`PAGAMENTOS.md`**.

### 9. Empresa — `/companies`
Dados do escritório (a "empresa dona"). Acesso: `ADMIN`/`CONTADOR`.

### 10. Usuários — `/users`
Gestão de usuários e papéis. Acesso: `ADMIN`.

### 11. Auditoria — `/audit-logs`
Trilha de ações relevantes do sistema. Acesso: `ADMIN`.

### 12. Portal do cliente — `/portal`
Área simplificada para o papel `CLIENTE` acompanhar o próprio status.

### 13. Configurações
- **Webhooks de saída** — `/webhooks/subscriptions`: cadastre URLs para o
  Nalitech notificar sistemas externos em eventos.
- **API Keys** — `/api-keys`: chaves para integrações programáticas.
- **Notificações**: por padrão só o canal **e-mail** está ativo. Os canais
  **WhatsApp** e **Push** estão **ocultos/não implementados** nativamente
  (WhatsApp é feito via fluxo externo n8n — E20). Para reativar o WhatsApp em
  código, defina `notifications.whatsapp.enabled=true` no ambiente.

---

## Como funciona a conciliação

Conciliar é **casar** uma movimentação do extrato com sua contrapartida (outro
lançamento correspondente) para confirmar que aquele valor é legítimo e sabemos
a que ele se refere. No Nalitech isso acontece em **camadas em cascata**: a
primeira que encontrar um bom candidato vence.

### Quando dispara
Assim que o upload termina de normalizar as movimentações, um evento
(`MovimentacoesNormalizadasEvent`) aciona, de forma **assíncrona** e após o
commit, o `ReconciliationPipelineListener`, que chama o `MatchingService` para
cada movimentação nova.

### As 4 camadas (nesta ordem)

1. **Match exato (`EXATA`, score 100)**
   Procura outra movimentação da mesma empresa com **mesma data e mesmo valor**.
   Se achar, casa direto — é o caso mais confiável.

2. **Match por similaridade (`SIMILARIDADE`, score = semelhança × 100)**
   Entre movimentações de **mesmo valor**, compara as **descrições** por
   similaridade textual (`StringSimilarity.ratio`). Se a melhor semelhança for
   **≥ 0,70 (70%)**, casa por similaridade. Serve para pequenas variações de
   texto (abreviações, espaços, etc.).

3. **Match por regra (`REGRA`, score 80)**
   Aplica as **regras de conciliação ativas** da empresa. Uma regra pode exigir
   que a descrição **contenha** um texto e/ou que o valor seja **≥ um mínimo**.
   Se a movimentação satisfaz a regra, é conciliada por ela (sem apontar uma
   contrapartida específica).

4. **Sem correspondência (`MANUAL`, score 0)**
   Se nenhuma camada resolveu, a conciliação fica **pendente para revisão
   manual** e dispara um `ConciliacaoPendenteEvent` (usado para notificar).

> Em todos os casos a movimentação fica com status `CONCILIACAO_PENDENTE` e é
> criado um registro de conciliação com status `PENDENTE`. Ou seja, **o
> automático apenas sugere**; a confirmação é humana.

### Revisão manual (a decisão final é do contador)
No módulo **Conciliação** você vê as pendências (`GET /reconciliations/pending`),
cada uma com a **camada** que a gerou, o **score** e o **motivo**. Então:

- **Confirmar** (`POST /reconciliations/{id}/confirm`): a conciliação vira
  `CONFIRMADO`, a movimentação vira `CONCILIADO` e dispara
  `ConciliacaoConfirmadaEvent` (pode carregar uma conta contábil sugerida para a
  classificação seguinte).
- **Rejeitar** (`POST /reconciliations/{id}/reject`): a conciliação vira
  `REJEITADO` e a movimentação **volta** para `NORMALIZADO` (entra de novo no
  fluxo para novo tratamento).

### Resumo dos estados

| Entidade | Estados |
|---|---|
| Movimentação | `NORMALIZADO` → `CONCILIACAO_PENDENTE` → `CONCILIADO` → `CLASSIFICADO` |
| Conciliação | `PENDENTE` → `CONFIRMADO` ou `REJEITADO` |
| Camadas de match | `EXATA` (100) · `SIMILARIDADE` (≥70%) · `REGRA` (80) · `MANUAL` (0) |

### Dica de configuração
Quanto mais bem cadastradas as **regras de conciliação** (e depois as **regras
de plano de contas**), menos itens caem em revisão manual. Comece observando o
que mais aparece como `MANUAL` e crie regras para esses padrões.

---

## Referência rápida de rotas

| Módulo | Base |
|---|---|
| Autenticação | `/auth` |
| Clientes | `/clients` |
| Uploads | `/uploads` |
| Movimentações | `/movements` |
| Conciliação | `/reconciliations` |
| Plano de contas / regras | `/chart-of-accounts`, `/account-rules` |
| Parametrização De/Para | `/parametrization` |
| Contas bancárias (partida dobrada) | `/bank-accounts` |
| Centros de custo | `/cost-centers` |
| Filiais | `/branches` |
| Financiamentos/empréstimos | `/loan-contracts` |
| Layouts de importação | `/import-layouts` |
| Exportação | `/layouts` |
| Agenda fiscal | `/fiscal-obligations` |
| Financeiro do escritório | `/office` (ver `PAGAMENTOS.md`) |
| Empresa | `/companies` |
| Usuários | `/users` |
| Auditoria | `/audit-logs` |
| Portal do cliente | `/portal` |
| Webhooks (saída) | `/webhooks/subscriptions` |
| API Keys | `/api-keys` |
| Dashboard | `/dashboard` |
