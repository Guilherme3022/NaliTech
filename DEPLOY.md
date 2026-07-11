# Nalitech — Guia de execução e deploy

> As decisões de infraestrutura (qual provedor, quando migrar de plano) são suas
> — ver `01-infra.md`. Este guia **explica as opções e os passos**; ele não
> escolhe nem executa nada em nuvem por você.

---

## 1. Rodar localmente

### Pré-requisitos
- **Docker** (Desktop) e **JDK 21** (para rodar pela IDE).

### Opção A — Backend pela IDE (recomendado para desenvolver)

1. Suba só a infra:
   ```bash
   docker compose up -d postgres redis rabbitmq minio mailpit
   ```
2. Rode a classe `NalitechApplication` no IntelliJ (▶).
   - Não precisa de `.env`: o `application.yml` já tem defaults de `localhost`
     que batem com as portas do compose.
3. Pronto:
   - API/Swagger: http://localhost:8080/swagger-ui.html
   - Health: http://localhost:8080/actuator/health
   - E-mails enviados (reset de senha etc.): http://localhost:8025 (Mailpit)

### Opção B — Tudo em container

```bash
docker compose up -d --build
```
Sobe infra + backend (build via `Dockerfile`). O backend fica em :8080.

### Primeiro acesso
Na primeira subida é criado um **admin**:
`admin@nalitech.local` / `admin123` (troque via `DEFAULT_ADMIN_*`).

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@nalitech.local","password":"admin123"}'
```

### Painéis úteis
| Serviço | URL | Login |
|---|---|---|
| Swagger | http://localhost:8080/swagger-ui.html | — |
| MinIO | http://localhost:9001 | nalitech / nalitech123 |
| RabbitMQ | http://localhost:15672 | nalitech / nalitech |
| Mailpit | http://localhost:8025 | — |

---

## 2. O que falta / o que é opcional

| Item | Status | Observação |
|---|---|---|
| Backend, Postgres, Storage (MinIO/S3) | ✅ funcional | núcleo do sistema |
| Redis | ⚪ opcional | starter incluso, mas **sem uso funcional** hoje (não há `@Cacheable`). Pode omitir em produção. |
| RabbitMQ | ⚪ opcional | o pipeline roda via `@Async` (síncrono). Só necessário se um dia migrar para fila real. |
| OCR (Tesseract) | ⚠️ depende do ambiente | precisa do binário + `tessdata` na máquina/imagem. Sem ele, PDF escaneado/imagem degrada (confiança baixa) sem quebrar o fluxo. Ver seção 4. |
| Gateway de pagamento | ⚠️ simulado | provedor padrão `SIMULADO`. Para cobrança real, configurar `ASAAS_API_TOKEN` (E12). |
| E-mail real | ⚪ opcional | em dev vai para o Mailpit. Em produção, apontar `MAIL_*` para um SMTP (ex: Resend/SendGrid). |
| Frontend | ✅ repo separado | fica em `../NalitechFront` (React + Vite). Deploy coberto na seção 3.5. |

---

## 3. Subir o sistema todo online de graça (trilha do `01-infra.md`)

> Aqui o objetivo é colocar **backend + frontend** no ar sem custo. A arquitetura
> final fica assim:
>
> ```
>   [ Navegador do usuário ]
>            │  HTTPS
>            ▼
>   [ Frontend estático ]  ──── chamadas /api ───►  [ Backend Spring Boot ]
>   (Vercel / Netlify /                          (Render / Cloud Run / Oracle)
>    Cloudflare Pages)                                     │        │
>                                                          ▼        ▼
>                                                   [ Neon Postgres ] [ R2 storage ]
> ```
>
> Ordem recomendada: **(1) banco → (2) storage → (3) backend → (4) frontend**.
> O frontend precisa da URL pública do backend, e o backend precisa liberar o
> domínio do frontend no CORS — por isso eles são os dois últimos passos e se
> "apontam" um para o outro no final.

O backend é um container Spring Boot que lê **tudo de variáveis de ambiente** —
então o deploy é: escolher onde rodar o container + um Postgres gerenciado + um
storage S3-compatible. Redis/RabbitMQ podem ficar de fora no MVP. O frontend é um
build estático (HTML/CSS/JS) — vai em qualquer host de site estático grátis.

### Passo a passo (o que **você** precisa fazer)

**a) Banco — Neon (Postgres free)**
1. Crie um projeto no Neon e copie a connection string.
2. No deploy, defina:
   - `DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require`
   - `DB_USER=<user>` / `DB_PASSWORD=<senha>`
   - (o Flyway cria o schema sozinho na 1ª subida)

**b) Storage — Cloudflare R2 (10 GB free, S3-compatible)**
1. Crie um bucket no R2 (crie pelo painel para não depender de permissão de criar bucket).
2. Gere um token de API (Access Key / Secret).
3. Defina:
   - `STORAGE_ENDPOINT=https://<accountid>.r2.cloudflarestorage.com`
   - `STORAGE_ACCESS_KEY=...` / `STORAGE_SECRET_KEY=...`
   - `STORAGE_BUCKET=<seu-bucket>` / `STORAGE_REGION=auto`
   - O código já usa path-style, então funciona igual ao MinIO local.

**c) Backend — onde rodar o container Spring Boot**

Essa é a decisão que mais afeta **experiência** e **custo** (ver `CUSTOS.md`).
O backend é um container que lê tudo de variáveis de ambiente; qualquer host que
rode Docker serve. O que muda entre as opções é **se a instância dorme** e
**quanta RAM** você tem — e Spring Boot é "faminto" de memória.

| Opção | Sempre ligado? | RAM (free) | Quando usar |
|---|---|---|---|
| **Render** (free web service) | ❌ dorme após ~15 min ocioso | 512 MB | Testar/MVP. 1º acesso após dormir leva ~30–50s (cold start). |
| **Google Cloud Run** | ⚪ escala a zero (paga por uso) | configurável | Quase grátis em pouco tráfego; usa o `Dockerfile`. Também tem cold start. |
| **Oracle Cloud "Always Free"** | ✅ **sim, de verdade** | até **24 GB** (ARM Ampere A1) | Uso real (escritório): VM que **não dorme**. Custo: mais setup manual. |
| **Railway / Fly.io** | ✅ (pago) | conforme plano | ~US$ 5–7/mês; simples e sempre-ligado, sem dor de cabeça. |

> ⚠️ **Railway não tem mais tier gratuito contínuo** (dá só um crédito inicial de
> teste e depois cobra). Para "grátis de verdade e sempre ligado", a escolha é
> **Oracle Always Free**; para "grátis e simples aceitando cold start", **Render**
> ou **Cloud Run**.

**Passos (valem para Render / Cloud Run / Railway — plataformas com deploy por Docker):**
1. Conecte este repositório. A plataforma detecta o `Dockerfile` e faz o build.
2. Cadastre as variáveis de ambiente (seções a, b + as de segurança abaixo).
3. Health check da plataforma: aponte para `/actuator/health`.
   - Como você provavelmente **não** terá Redis/RabbitMQ, defina:
     `REDIS_HEALTH_ENABLED=false` e `RABBIT_HEALTH_ENABLED=false`
     (senão o health fica DOWN e a plataforma marca o serviço como não saudável).
4. **Memória (importante nos free tiers de 512 MB):** limite o heap da JVM para
   não estourar o container e sofrer `OOMKilled`. Defina:
   `JAVA_TOOL_OPTIONS=-Xmx350m -XX:MaxRAMPercentage=70`
   Com Oracle ARM (24 GB) isso deixa de ser preocupação.
5. Anote a **URL pública** que a plataforma gera (ex:
   `https://nalitech-api.onrender.com`). Você vai precisar dela no passo 3.5
   (frontend) e no `CORS_ALLOWED_ORIGINS`.

**Especificidades por opção:**
- **Render:** "New Web Service → From a repository", runtime **Docker**. O plano
  free dorme; se o escritório for usar de verdade, suba para o plano pago
  (~US$ 7/mês) para ficar sempre ligado e evitar o cold start no login.
- **Google Cloud Run:** `gcloud run deploy --source .` (usa o `Dockerfile`).
  Defina `--min-instances=0` (grátis, com cold start) ou `--min-instances=1`
  (sem cold start, mas passa a custar). Cloud Run injeta a porta em `$PORT` —
  o Spring já lê `SERVER_PORT`, então mapeie `SERVER_PORT=$PORT` (ou 8080).
- **Oracle Always Free:** crie uma VM **Ampere A1 (ARM)**, instale Docker,
  rode `docker compose` (só o serviço `backend`) ou o container isolado, e
  coloque um **nginx/Caddy** na frente para TLS. É o único 100% grátis e
  sempre-ligado, ao custo de você administrar o servidor.

> Cold start em números: uma instância que dormiu precisa **acordar + subir o
> Spring Boot** (~15–40s no total). Para demonstração tudo bem; para um cliente
> logando no dia a dia, é ruim — por isso, em uso real, prefira uma opção
> **sempre-ligada** (Oracle grátis ou um plano pago barato).

**d) Variáveis de segurança obrigatórias em produção**
- `JWT_SECRET` = segredo forte com **no mínimo 32 caracteres**.
- `NALITECH_ENCRYPTION_KEY` = **exatamente 32 caracteres** (senão o app não sobe).
- `DEFAULT_ADMIN_PASSWORD` = troque o padrão.
- `CORS_ALLOWED_ORIGINS` = domínio do seu frontend (ex: `https://nalitech.vercel.app`).
  Você só vai saber esse valor depois do passo 3.5 — pode deixar provisório e
  voltar aqui para ajustar (ver 3.6).

**e) (Opcional) DNS/HTTPS — Cloudflare**
- Aponte um subdomínio (ex: `api.seudominio.com.br`) para o serviço do Railway/Render.

**f) (Opcional) Cache/fila gerenciados** — só quando precisar:
- Redis → Upstash (free). Atenção: exige **senha + TLS**; a config atual de Redis
  não define senha/TLS, então precisaria de ajuste. Como o Redis hoje é opcional,
  o mais simples é deixar desativado.
- Fila → CloudAMQP (free "Little Lemur"), só se migrar o pipeline para fila real.

### 3.5. Frontend — Vercel (ou Netlify / Cloudflare Pages), free

O frontend fica no repositório irmão `../NalitechFront` (React + Vite). Ele
é só arquivos estáticos — qualquer host de site estático serve. Passos na Vercel:

1. **Suba `../NalitechFront` para o GitHub** (é um repo git próprio).
2. Na Vercel, **New Project → importe esse repositório**. Ela detecta o Vite sozinha:
   - Build command: `npm run build`
   - Output directory: `dist`
   - (já há um `vercel.json` no repo que faz o *fallback* de rotas do React
     Router para `index.html` — sem ele, dar F5 numa rota interna daria 404.)
3. Em **Settings → Environment Variables**, crie:
   - `VITE_API_BASE_URL` = a **URL pública do backend** do passo (c), ex:
     `https://nalitech-api.up.railway.app`
   > ⚠️ O Vite embute variáveis `VITE_*` no bundle **em tempo de build**. Se você
   > mudar essa variável depois, precisa **refazer o deploy** (redeploy) para valer.
4. Deploy. A Vercel gera a URL pública (ex: `https://nalitech.vercel.app`).

> **Alternativa Docker + nginx (Railway/Render):** o frontend também tem
> `Dockerfile` + `nginx.conf`. Nesse caso a URL da API entra como **build arg**:
> `--build-arg VITE_API_BASE_URL=https://sua-api...`. Em Railway/Render, configure
> esse build arg na plataforma. O nginx já faz o fallback de SPA para `index.html`.

### 3.6. Ligar os dois (o passo que todo mundo esquece)

Depois que o frontend tem URL pública, volte no **backend** e ajuste o CORS:

- `CORS_ALLOWED_ORIGINS=https://nalitech.vercel.app`
  (o domínio exato do frontend, com `https://`, sem barra no final; vários
  domínios podem ser separados por vírgula).

Reinicie/redeploy o backend para aplicar. Sem isso, o navegador **bloqueia** as
chamadas do frontend com erro de CORS, mesmo com o backend no ar.

Como o frontend chama a API pela URL absoluta (`VITE_API_BASE_URL`), **não** existe
proxy em produção (o proxy `/api → localhost:8080` do `vite.config.ts` só vale em
desenvolvimento). Por isso o CORS no backend é obrigatório em produção.

### Resumo mínimo para o MVP online
**Neon (Postgres) + R2 (storage) + backend (Render/Cloud Run grátis, ou Oracle
Always Free para não dormir) + Vercel (frontend)** — com
`REDIS_HEALTH_ENABLED=false`, `RABBIT_HEALTH_ENABLED=false`, `JAVA_TOOL_OPTIONS`
limitando o heap nos free tiers de 512 MB, `VITE_API_BASE_URL` apontando pro
backend e `CORS_ALLOWED_ORIGINS` apontando pro frontend. Custo ≈ R$ 0.

> Detalhes de custo por item e quando migrar de plano: ver `CUSTOS.md`.

---

## 4. Nota sobre OCR em produção
A imagem `Dockerfile` atual **não** instala o Tesseract. Se você precisar de OCR
de PDFs escaneados/imagens em produção, será necessário adicionar ao estágio de
runtime do `Dockerfile` os pacotes `tesseract-ocr` e `tesseract-ocr-por` e apontar
`OCR_TESSDATA_PATH`. Para o MVP (arquivos CSV/OFX/XLSX/PDF com texto nativo), o OCR
não é necessário — o pipeline funciona sem ele.

---

## 5. IA — sugestão automática de conta contábil

O Nalitech sugere a conta contábil de cada movimentação. São **dois modos**,
escolhidos pela variável `AI_PROVIDER`:

| Modo | `AI_PROVIDER` | Como funciona | Custo |
|---|---|---|---|
| **Heurística** (padrão) | `HEURISTICA` | Aprende com o histórico de decisões do contador (descrições semelhantes já classificadas). Determinística. | **US$ 0** |
| **IA (LLM)** | `IA` | Envia a descrição + o plano de contas a um modelo de linguagem, que escolhe a conta. | pago por uso (ver abaixo) |

> **A ordem é sempre:** regra explícita → (IA, se ligada) → histórico → manual.
> Se `AI_PROVIDER=IA` mas a `AI_API_KEY` estiver vazia ou a chamada falhar, o
> sistema **cai automaticamente na heurística** (nunca quebra o fluxo).

### 5.1. Como ligar a IA

No `.env`/ambiente do backend:

```env
AI_PROVIDER=IA
AI_API_URL=https://api.openai.com/v1     # endpoint compativel com a API da OpenAI
AI_API_KEY=sk-...                        # sua chave (NUNCA commitar)
AI_MODEL=gpt-4o-mini                     # modelo barato e bom para classificar
```

O cliente usa o **formato da API da OpenAI** (`/chat/completions`), então funciona
com vários provedores — basta trocar `AI_API_URL`/`AI_MODEL`:

| Provedor | `AI_API_URL` | Observação |
|---|---|---|
| **OpenAI** | `https://api.openai.com/v1` | `gpt-4o-mini` é barato e suficiente |
| **Groq** | `https://api.groq.com/openai/v1` | tem tier gratuito generoso; modelos Llama |
| **OpenRouter** | `https://openrouter.ai/api/v1` | agrega vários modelos (alguns free) |
| **Ollama (self-host)** | `http://localhost:11434/v1` | roda local, **custo zero**, sem enviar dados p/ fora |

### 5.2. Quanto custa (ordem de grandeza)

> Preços mudam; confirme no provedor. Referência (meados de 2025) para o
> `gpt-4o-mini`: ~US$ 0,15 por 1M tokens de entrada e ~US$ 0,60 por 1M de saída.

Cada sugestão manda a descrição do lançamento + o plano de contas (limitado a 200
contas) → algo como **1–3 mil tokens de entrada** e pouquíssimos de saída (só o
código). Ou seja, ~**US$ 0,0003–0,0005 por sugestão** no gpt-4o-mini.

Pontos que **reduzem muito** o custo real:
- A IA só é chamada quando **não há regra nem histórico** que resolva. Conforme o
  contador parametriza, a maioria passa a ser resolvida de graça (regra/histórico)
  e a IA quase não é acionada.
- Para um escritório de ~20 clientes, isso costuma dar **poucos dólares/mês** — e
  **US$ 0** se usar Groq (free tier) ou Ollama (local).

**Recomendação:** comece em `HEURISTICA` (grátis e já aprende sozinho). Ligue a
IA só se quiser acelerar a parametrização inicial de clientes novos; e considere
**Groq/Ollama** para não ter custo. Avalie também privacidade: enviar descrições
de extratos a um LLM externo pode ser sensível — Ollama (local) evita isso.

---

## 6. Checklist rápido de go-live

**Backend**
- [ ] `DB_URL/DB_USER/DB_PASSWORD` (Neon, com `sslmode=require`)
- [ ] `STORAGE_*` (R2, bucket criado)
- [ ] `JWT_SECRET` forte (≥32) e `NALITECH_ENCRYPTION_KEY` (=32)
- [ ] `DEFAULT_ADMIN_PASSWORD` trocado
- [ ] `CORS_ALLOWED_ORIGINS` = domínio do frontend (preenchido após o deploy do front)
- [ ] `REDIS_HEALTH_ENABLED=false`, `RABBIT_HEALTH_ENABLED=false` (se sem esses serviços)
- [ ] `JAVA_TOOL_OPTIONS=-Xmx350m -XX:MaxRAMPercentage=70` (se free tier de 512 MB)
- [ ] Host **sempre-ligado** se for uso real (Oracle Always Free ou plano pago) — evita cold start
- [ ] Health check da plataforma → `/actuator/health`
- [ ] URL pública da API anotada

**Frontend** (`../NalitechFront`)
- [ ] Repo no GitHub e importado na Vercel (ou host estático equivalente)
- [ ] `VITE_API_BASE_URL` = URL pública da API
- [ ] Redeploy feito **depois** de setar/alterar `VITE_API_BASE_URL`
- [ ] URL pública do front colocada no `CORS_ALLOWED_ORIGINS` do backend (e backend reiniciado)

**Fumça final**
- [ ] Abrir o front, logar com o admin, ver o Dashboard carregar sem erro de CORS/401
- [ ] Fazer um upload de teste e ver o status atualizar
