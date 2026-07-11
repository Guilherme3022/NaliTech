# Custos de Infraestrutura — Nalitech

Estimativa de custo para rodar o Nalitech em produção, com foco no cenário de
um **escritório de contabilidade com ~20 clientes**.

> ⚠️ Preços de nuvem mudam com frequência. Os valores aqui são **ordens de
> grandeza** (referência: meados de 2025). Confirme na hora de contratar.

---

## 1. O conceito que mais impacta a conta: dois tipos de "storage"

Existem dois lugares para guardar dados, com preços **muito** diferentes. Não
confundir os dois é o que evita gastar demais.

| Tipo | O que guarda | Preço por GB | Serviço no Nalitech |
|---|---|---|---|
| **Banco (Postgres/Neon)** | Linhas: movimentações, clientes, conciliações, logs de auditoria | **Caro** (~US$ 1,5–4/GB-mês) | Neon |
| **Object storage (S3/R2)** | Os **arquivos**: extratos PDF/OFX, documentos, imagens do OCR | **Barato** (~US$ 0,015–0,023/GB-mês) | Cloudflare R2 / MinIO |

**Regra de ouro:** os arquivos pesados vão para o **object storage** (barato),
não para o banco. O Nalitech já faz isso (`shared/storage`, MinIO em dev → R2/S3
em produção). Por isso **você quase nunca precisa "aumentar o storage do Neon"**:
o banco fica pequeno; quem cresce é o R2, e crescer no R2 é barato.

---

## 2. Quanto de dado 20 clientes geram (estimativa)

- **Banco (Neon):** ~5–10 mil movimentações/mês, ~1 KB por linha →
  **~0,2–0,5 GB por ano**. Ou seja, **5 GB duram anos**; começar pequeno é
  seguro.
- **Arquivos (R2):** ~20 clientes × 10–20 arquivos/mês × 1–2 MB →
  **~0,3–0,8 GB/mês** ≈ **4–10 GB/ano**. Cabe no free tier de 10 GB do R2, e
  passando disso custa centavos (R2 não cobra egress, diferente da S3).

---

## 3. Estimativa de custo mensal (20 clientes)

| Item | Opção | Custo/mês (aprox.) |
|---|---|---|
| **Frontend** | Vercel / Netlify / Cloudflare Pages (free) | **US$ 0** |
| **Object storage** | Cloudflare R2 (10 GB free) | **US$ 0–1** |
| **E-mail** | Resend / SendGrid free tier | **US$ 0** |
| **Banco** | Neon free (0,5 GB) *ou* plano pago com backup | **US$ 0–22** |
| **Backend** (item que mais pesa) | instância sempre-ligada 512 MB–1 GB | **US$ 7–15** |
| **RabbitMQ / Redis** | opcional (pipeline roda `@Async`) | **US$ 0** |
| | **TOTAL realista** | **≈ US$ 15–40/mês (~R$ 90–230)** |

**Leitura prática:**
- O **maior custo é o backend (compute)**, não o storage.
- **Neon 5 GB (US$ 22) é exagero para começar** — dá para iniciar no free.
- **"Aumentar storage" quase sempre = R2** (barato: ~US$ 0,15 por 10 GB extra),
  não Neon.

---

## 4. Dá para subir o backend inicial de graça?

**Sim** — mas o tipo de "grátis" muda a experiência do usuário.

| Opção | Sempre ligado? | RAM | Trade-off |
|---|---|---|---|
| **Render (free web service)** | ❌ dorme após ~15 min | 512 MB | Fácil de subir. "Cold start": 1º acesso após ocioso leva ~30–50s. 512 MB é apertado para Spring Boot (ajustar `-Xmx`). |
| **Google Cloud Run** | ⚪ escala a zero (paga por uso) | configurável | Quase grátis em pouco tráfego; usa o `Dockerfile` existente. Também tem cold start. |
| **Oracle Cloud "Always Free"** | ✅ **sim, de verdade** | até **24 GB** (ARM Ampere A1) | Melhor grátis: VM real que não dorme. Custo: você administra o servidor (mais setup) e o cadastro é burocrático. |

**Recomendações:**
- **Só testar / MVP:** Render free resolve na hora.
- **Escritório real (20 clientes):** o "dormir" dá péssima UX (login esperando
  ~40s). Prefira **Oracle Always Free** (grátis e sempre ligado, porém mais
  trabalho) **ou** pague **~US$ 7/mês** por uma instância pequena sempre-ligada e
  não pense mais nisso.
- **Dica de memória:** Spring Boot é "faminto" de RAM. Em 512 MB, limite o heap
  (ex.: `JAVA_TOOL_OPTIONS=-Xmx350m`) e desative o que não usa. Com Oracle ARM
  (24 GB) isso deixa de ser problema.

---

## 5. Trilha "grátis" completa (MVP online por ~R$ 0)

Conforme o `DEPLOY.md`: **Neon (Postgres) + Cloudflare R2 (storage) +
Render/Cloud Run/Oracle (backend) + Vercel (frontend)**. Redis/RabbitMQ ficam de
fora (o pipeline roda `@Async`). OCR é opcional (degrada sem quebrar).

Ordem: **(1) banco → (2) storage → (3) backend → (4) frontend**.

---

## 6. Variáveis de ambiente por provedor

Mapeie as variáveis do `.env.example` para cada serviço em produção:

### Banco — Neon
```env
# Neon te dá uma connection string; converta para o formato JDBC:
DB_URL=jdbc:postgresql://<host>.neon.tech/<db>?sslmode=require
DB_USER=<usuario>
DB_PASSWORD=<senha>
```

### Storage — Cloudflare R2 (S3-compatible)
```env
STORAGE_ENDPOINT=https://<accountid>.r2.cloudflarestorage.com
STORAGE_ACCESS_KEY=<access-key-do-token-R2>
STORAGE_SECRET_KEY=<secret-key-do-token-R2>
STORAGE_BUCKET=nalitech-files
STORAGE_REGION=auto
```

### E-mail — Resend/SendGrid (SMTP)
```env
MAIL_HOST=<smtp-do-provedor>
MAIL_PORT=587
MAIL_USER=<usuario>
MAIL_PASSWORD=<senha/api-key>
MAIL_FROM=no-reply@seudominio.com.br
OFFICE_NOTIFICATION_EMAIL=escritorio@seudominio.com.br
```

### Segurança (obrigatório trocar em produção — ver SEGURANCA.md)
```env
JWT_SECRET=<segredo-forte-min-32-bytes>
NALITECH_ENCRYPTION_KEY=<chave-forte-32-chars>
CORS_ALLOWED_ORIGINS=https://seu-frontend.vercel.app
DEFAULT_ADMIN_EMAIL=<seu-email-admin>
DEFAULT_ADMIN_PASSWORD=<senha-forte-e-troque-no-1o-acesso>
```

### Opcionais (deixar de fora no MVP)
```env
# Redis e RabbitMQ nao sao necessarios no inicio (pipeline roda @Async).
# Pagamento: comecar em SIMULADO; ver PAGAMENTOS.md para ligar o Asaas.
PAYMENT_PROVIDER=SIMULADO
```

---

## 7. Quando o custo sobe (gatilhos futuros)

- **Muito mais clientes / anexos grandes:** cresce o R2 (barato) e, com muito
  volume, o Neon sobe de plano (moderado).
- **Precisar de fila real / assíncrono robusto:** adicionar RabbitMQ/Redis
  gerenciado (+US$ 5–15/mês).
- **OCR pesado em produção:** exige binário Tesseract + `tessdata` na imagem
  (mais CPU/RAM no backend). Ver `DEPLOY.md` seção 4.
- **Backend sempre-ligado com mais RAM:** ao sair do free, ~US$ 7–25/mês
  conforme RAM/always-on.
