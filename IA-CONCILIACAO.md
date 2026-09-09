# IA na Conciliação — guia de configuração na subida

Este guia explica como **ligar e configurar a IA que valida as conciliações
pendentes** (as que o algoritmo não conseguiu casar sozinho). É a camada
"paliativa": antes de o contador resolver na mão, uma IA varre o que sobrou e
sugere correspondências.

> Resumo em uma linha: a conciliação automática roda **sem IA** (rápida e grátis);
> a IA entra **sob demanda**, por um botão, de forma **assíncrona**.

---

## Sumário

- [1. Como funciona (visão geral)](#1-como-funciona-visão-geral)
- [2. Escolha do provedor (grátis, hospedado)](#2-escolha-do-provedor-grátis-hospedado)
- [3. Configuração mínima na subida](#3-configuração-mínima-na-subida)
- [4. Variáveis de ambiente (referência)](#4-variáveis-de-ambiente-referência)
- [5. Endpoints (para o front)](#5-endpoints-para-o-front)
- [6. Custo e privacidade](#6-custo-e-privacidade)
- [7. Ajuste fino (tolerâncias e confiança)](#7-ajuste-fino-tolerâncias-e-confiança)
- [8. Operação e escala](#8-operação-e-escala)
- [9. Troubleshooting](#9-troubleshooting)

---

## 1. Como funciona (visão geral)

A conciliação roda em **camadas em cascata** — a primeira que acha um bom
candidato vence:

```
EXATA → SIMILARIDADE → REGRA → APROXIMADA → [MANUAL]
                                               │
                                               ▼  (botão do usuário, assíncrono)
                                              IA  → conciliado (sugestão) / continua MANUAL
```

- As 4 primeiras camadas são **algorítmicas, grátis e síncronas** (rodam no
  processamento do upload). `APROXIMADA` já tolera centavos no valor e alguns
  dias na data (compensação D+n).
- O que sobra fica `MANUAL`. **A IA não roda automática** — ela é acionada pelo
  botão **"Validar pendências com IA"**, que dispara uma **varredura assíncrona**
  (sweep) sobre os itens `MANUAL`.
- A IA **só sugere**; a confirmação continua sendo humana.
- **Memória:** cada item avaliado pela IA é marcado (`ia_tentada`). Rodar o sweep
  de novo **não re-chama** a IA para o que ela já analisou — economiza chamadas.

Cada chamada à IA é **isolada (stateless)**: ela recebe a movimentação + uma
lista curta de candidatos e responde qual bate, com confiança e justificativa.
Não há "contexto por conciliação" nem histórico de conversa — a mesma conta/chave
atende todos os clientes com segurança.

---

## 2. Escolha do provedor (grátis, hospedado)

A IA usa o **formato da API da OpenAI** (`/chat/completions`), então funciona com
vários provedores só trocando `AI_API_URL`/`AI_MODEL`. Para um **produto vendável
com volume**, use um provedor **hospedado** (evite rodar local):

| Provedor | `AI_API_URL` | Custo | Observação |
|---|---|---|---|
| **Groq** ⭐ | `https://api.groq.com/openai/v1` | **Free tier** generoso | Recomendado. Rápido, modelos Llama. Sujeito a limites de requisições/min. |
| **Google Gemini** | `https://generativelanguage.googleapis.com/v1beta/openai` | Free tier | Endpoint compatível com OpenAI. |
| **OpenRouter** | `https://openrouter.ai/api/v1` | Alguns modelos **free** | Agrega vários modelos; bom para fallback. |
| **OpenAI** | `https://api.openai.com/v1` | Pago por uso | Barato (`gpt-4o-mini`), útil se o free tier limitar. |
| ~~Ollama (local)~~ | `http://localhost:11434/v1` | Zero | **Não recomendado para SaaS** (não escala para volume). |

> **Recomendação para produção:** comece no **Groq** (free). Se o volume estourar
> os limites do free tier, aponte para um pago barato (OpenAI `gpt-4o-mini`) —
> **basta trocar 3 variáveis**, sem mexer no código.

Modelos sugeridos por provedor:

| Provedor | `AI_MODEL` sugerido |
|---|---|
| Groq | `llama-3.3-70b-versatile` (ou `llama-3.1-8b-instant` p/ mais velocidade) |
| Gemini | `gemini-2.0-flash` |
| OpenRouter | um modelo `:free` (ex.: `meta-llama/llama-3.3-70b-instruct:free`) |
| OpenAI | `gpt-4o-mini` |

---

## 3. Configuração mínima na subida

No `.env`/ambiente do backend, para ligar a IA de conciliação com **Groq (grátis)**:

```env
# Liga a camada de IA na conciliação
RECONCILIATION_AI_ENABLED=true

# Provedor hospedado (Groq free tier)
AI_API_URL=https://api.groq.com/openai/v1
AI_API_KEY=gsk_...                       # sua chave do Groq (NUNCA commitar)
AI_MODEL=llama-3.3-70b-versatile
```

Só isso. As demais variáveis (`RECON_*`) têm defaults sensatos — ver seção 4.

> `AI_API_KEY`/`AI_API_URL`/`AI_MODEL` são **compartilhadas** com a IA de
> classificação de conta (seção 5 do `DEPLOY.md`). Ligar uma **não** liga a outra:
> a de conta usa `AI_PROVIDER=IA`; a de conciliação usa `RECONCILIATION_AI_ENABLED=true`.

Se `RECONCILIATION_AI_ENABLED=false` (padrão) ou a chave estiver ausente/ inválida,
o botão de varredura responde com um erro claro e **nada quebra** — a conciliação
manual segue normal.

---

## 4. Variáveis de ambiente (referência)

| Variável | Default | O que faz |
|---|---|---|
| `RECONCILIATION_AI_ENABLED` | `false` | Liga/desliga a camada de IA na conciliação. |
| `AI_API_URL` | `https://api.openai.com/v1` | Endpoint compatível com a API da OpenAI. |
| `AI_API_KEY` | *(vazio)* | Chave do provedor. Vazio = IA fica indisponível. |
| `AI_MODEL` | `gpt-4o-mini` | Modelo usado. |
| `RECON_DATE_WINDOW_DAYS` | `3` | Janela de dias no match **aproximado** (algorítmico). |
| `RECON_VALUE_TOLERANCE` | `0.02` | Tolerância de valor (R$) no match aproximado. |
| `RECON_SIMILARITY_THRESHOLD` | `0.7` | Gate (0–1) da camada SIMILARIDADE. |
| `RECON_APPROX_DESC_THRESHOLD` | `0.5` | Gate (0–1) da descrição no match aproximado. |
| `RECON_AI_MIN_CONFIDENCE` | `70` | Confiança mínima (0–100) para aceitar a sugestão da IA. |
| `RECON_AI_MAX_CANDIDATES` | `20` | Máx. de candidatos enviados por chamada (controla custo/latência). |
| `RECON_AI_INLINE` | `false` | `true` roda a IA já no pipeline automático (não recomendado; a IA é lenta). |

---

## 5. Endpoints (para o front)

Todos sob `/reconciliations` (perfis `ADMIN`/`CONTADOR`/`AUXILIAR`).

| Método | Rota | Uso |
|---|---|---|
| `POST` | `/reconciliations/ai-sweep?clienteId=&competencia=` | Dispara a varredura por IA. Devolve o job com o `total` de pendências. |
| `GET` | `/reconciliations/ai-sweep/{jobId}` | Progresso de um job (para a **barra de carregamento**). |
| `GET` | `/reconciliations/ai-sweep` | Jobs ativos/recentes da empresa (para o **popup reaparecer** ao navegar). |
| `GET` | `/ai-usage?competencia=YYYY-MM` | Consumo de IA da empresa no mês (chamadas + tokens) para faturamento. |

`competencia` é `YYYY-MM` (opcional); `clienteId` é opcional. Sem filtros, varre
todas as pendências `MANUAL` da empresa.

**Formato do job** (`AiSweepJob`):

```json
{
  "jobId": "uuid",
  "status": "EXECUTANDO",       // EXECUTANDO | CONCLUIDO | ERRO | SEM_PENDENCIAS
  "total": 42,                  // pendências a analisar
  "processados": 17,            // já avaliadas → barra = processados/total
  "resolvidos": 9,              // quantas a IA conseguiu conciliar
  "clienteId": "uuid|null",
  "competencia": "2026-02-01|null",
  "iniciadoEm": "2026-03-01T12:00:00Z",
  "concluidoEm": "2026-03-01T12:01:30Z|null"
}
```

**Fluxo sugerido no front (estilo upload do Drive):**
1. Botão no topo da tela de conciliação → `POST /ai-sweep`.
2. Mostra um **popup no canto inferior direito** com a barra `processados/total`.
3. Faz *polling* do `GET /ai-sweep/{jobId}` a cada ~2s até `status != EXECUTANDO`.
4. Ao trocar de tela/recarregar, chama `GET /ai-sweep` para **reconstruir o popup**
   de jobs que ainda estão rodando (ou concluídos há pouco).
5. Ao concluir, recarrega a lista de pendências (as resolvidas saem de `MANUAL`).

> O backend também publica um **evento interno** de conclusão (`SweepIaConcluido`)
> — disponível para notificação nativa por e-mail/métricas, **sem n8n**. O aviso
> principal ao usuário é o próprio popup in-app.

---

## 6. Custo e privacidade

### Medição de consumo (para faturar por empresa)

Cada chamada de IA (conciliação **e** classificação de conta) é contabilizada por
empresa, em duas saídas:

- **Banco (durável, para faturamento):** agregado mensal em `ai_usage_monthly`
  (chamadas + tokens de entrada/saída), por empresa e feature
  (`CONCILIACAO` / `CLASSIFICACAO`). Consulta:
  `GET /ai-usage?competencia=YYYY-MM` (ADMIN/CONTADOR) — retorna o consumo da
  empresa autenticada. `competencia` opcional (sem ela, todos os meses).
- **Prometheus (dashboard operacional cross-tenant):** métricas
  `nalitech.ai.calls` e `nalitech.ai.tokens`, com tags `empresa`, `feature`
  (e `tipo` = entrada/saída nos tokens). Útil para Grafana ver todas as empresas.

> Fluxo de cobrança sugerido: no fechamento do mês, puxe
> `GET /ai-usage?competencia=YYYY-MM` de cada empresa, multiplique os tokens pelo
> preço do provedor (ou some as chamadas se estiver no free tier) e repasse na
> mensalidade. Como está no banco, o número é exato e sobrevive a restart.

### Notas de custo

- **Quando a IA é chamada:** só nos itens `MANUAL` **ainda não avaliados**. Depois
  da primeira varredura, os já vistos são pulados (memória `ia_tentada`).
- **Tamanho da chamada:** 1 movimentação + até `RECON_AI_MAX_CANDIDATES` candidatos
  → poucos tokens. Resposta é um JSON curto.
- **No Groq/OpenRouter (free tier): US$ 0**, respeitados os limites de req/min do
  provedor. Se estourar, o item apenas continua `MANUAL` (nada quebra) e pode ser
  reprocessado depois.
- **Privacidade/LGPD:** descrições de extratos são enviadas ao provedor externo.
  Avalie os termos do provedor. Se o dado for sensível demais para sair, mantenha
  `RECONCILIATION_AI_ENABLED=false` — as camadas algorítmicas continuam operando.

---

## 7. Ajuste fino (tolerâncias e confiança)

- **Muitos itens em `MANUAL` que "eram óbvios":** aumente `RECON_DATE_WINDOW_DAYS`
  (ex.: 5) e/ou `RECON_VALUE_TOLERANCE` (ex.: 0.05) para a camada aproximada casar
  mais **antes** de chegar na IA (mais barato).
- **IA sugerindo matches ruins:** suba `RECON_AI_MIN_CONFIDENCE` (ex.: 80). Só
  aceita quando a IA está bem segura.
- **IA deixando passar matches bons:** desça um pouco (ex.: 60) — lembrando que é
  sugestão para revisão humana.
- **Chamadas caras/lentas:** reduza `RECON_AI_MAX_CANDIDATES` (ex.: 10).

Todos os valores são lidos na subida (variáveis de ambiente). Não requer deploy de
código para ajustar.

---

## 8. Operação e escala

- **Reprocessar sem IA:** `POST /reconciliations/reprocess` re-roda as camadas
  algorítmicas nas pendências `MANUAL` — útil depois de cadastrar regras novas.
- **Assíncrono:** a varredura roda em *background* (thread pool do Spring). O
  usuário pode navegar enquanto processa.
- **Registro de jobs em memória:** o progresso do sweep é mantido **em memória da
  instância** (adequado para **1 instância**, o cenário atual). Jobs concluídos
  ficam visíveis por ~30 min e depois são limpos.
  - ⚠️ **Ao escalar para 2+ instâncias** atrás de load balancer, um `GET /ai-sweep`
    pode cair em outra instância e não enxergar o job. Quando chegar esse momento,
    migre o registro de jobs para **Redis** (já disponível no projeto). Enquanto
    for instância única, não é necessário.

---

## 9. Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| `POST /ai-sweep` retorna 400 "IA de conciliação está desligada" | `RECONCILIATION_AI_ENABLED=false` ou `AI_API_KEY` vazia | Ligue a flag e configure o provedor (seção 3). |
| Sweep conclui com `resolvidos: 0` | Sem candidatos plausíveis, ou confiança abaixo do mínimo | Afrouxe tolerâncias (seção 7) ou baixe `RECON_AI_MIN_CONFIDENCE`. |
| Muitos itens ficam `MANUAL` mesmo após IA | Free tier atingiu limite de req/min | Tente de novo mais tarde (itens já vistos são pulados) ou troque de provedor. |
| Logs `Falha ao consultar LLM` | URL/model/chave incorretos, ou provedor fora | Confira `AI_API_URL`/`AI_MODEL`/`AI_API_KEY`. A conciliação segue funcionando sem IA. |
| Popup some ao trocar de tela | Front não está usando `GET /ai-sweep` | Reconstrua o popup pela lista de jobs ativos ao montar a tela. |

---

**Ver também:** `DEPLOY.md` (seção 5 — IA de classificação de conta) e
`MODULOS.md` (seção "Como funciona a conciliação").
