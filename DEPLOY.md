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
>   (Vercel / Netlify /                                (Railway / Render)
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

**c) Backend — Railway ou Render (free)**
1. Conecte este repositório. Ambos detectam o `Dockerfile` e fazem o build.
2. Cadastre as variáveis de ambiente (seções a, b + as de segurança abaixo).
3. Health check da plataforma: aponte para `/actuator/health`.
   - Como você provavelmente **não** terá Redis/RabbitMQ, defina:
     `REDIS_HEALTH_ENABLED=false` e `RABBIT_HEALTH_ENABLED=false`
     (senão o health fica DOWN e a plataforma marca o serviço como não saudável).
4. Anote a **URL pública** que a plataforma gera (ex:
   `https://nalitech-api.up.railway.app`). Você vai precisar dela no passo 3.5
   (frontend) e no `CORS_ALLOWED_ORIGINS`.

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
**Neon (Postgres) + R2 (storage) + Railway/Render (backend) + Vercel (frontend)** —
com `REDIS_HEALTH_ENABLED=false`, `RABBIT_HEALTH_ENABLED=false`,
`VITE_API_BASE_URL` apontando pro backend e `CORS_ALLOWED_ORIGINS` apontando pro
frontend. Custo ≈ R$ 0.

---

## 4. Nota sobre OCR em produção
A imagem `Dockerfile` atual **não** instala o Tesseract. Se você precisar de OCR
de PDFs escaneados/imagens em produção, será necessário adicionar ao estágio de
runtime do `Dockerfile` os pacotes `tesseract-ocr` e `tesseract-ocr-por` e apontar
`OCR_TESSDATA_PATH`. Para o MVP (arquivos CSV/OFX/XLSX/PDF com texto nativo), o OCR
não é necessário — o pipeline funciona sem ele.

---

## 5. Checklist rápido de go-live

**Backend**
- [ ] `DB_URL/DB_USER/DB_PASSWORD` (Neon, com `sslmode=require`)
- [ ] `STORAGE_*` (R2, bucket criado)
- [ ] `JWT_SECRET` forte (≥32) e `NALITECH_ENCRYPTION_KEY` (=32)
- [ ] `DEFAULT_ADMIN_PASSWORD` trocado
- [ ] `CORS_ALLOWED_ORIGINS` = domínio do frontend (preenchido após o deploy do front)
- [ ] `REDIS_HEALTH_ENABLED=false`, `RABBIT_HEALTH_ENABLED=false` (se sem esses serviços)
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
