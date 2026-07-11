# Nalitech — Backend

Backend do **Nalitech**, um sistema de apoio a escritórios de contabilidade que
automatiza o caminho de um documento financeiro do upload até a exportação para os
sistemas contábeis do mercado: **upload → OCR → parsing → normalização → conciliação
bancária → classificação contábil → exportação**, com painel, financeiro do
escritório (cobrança PIX/boleto), agenda fiscal, notificações, auditoria e um
padrão único de integração via **webhooks + API keys** (pensado para o n8n).

Projeto pessoal, de estudo. Stack: **Java 21 + Spring Boot 3**.

---

## Sumário

- [Arquitetura](#arquitetura)
- [Stack](#stack)
- [Estrutura de pastas](#estrutura-de-pastas)
- [O pipeline de processamento](#o-pipeline-de-processamento)
- [Módulos e funcionalidades (épicos)](#módulos-e-funcionalidades-épicos)
- [Segurança](#segurança)
- [Integração externa (n8n)](#integração-externa-n8n)
- [Como rodar](#como-rodar)
- [Testes](#testes)
- [Observabilidade](#observabilidade)
- [Notas e limitações](#notas-e-limitações)

---

## Arquitetura

Organização por **módulos de negócio** (`modules/<modulo>/`), cada um com as camadas
`controller / service / usecase / repository / entity / dto / mapper / event`. O código
transversal (segurança, storage, exceções, utilidades) fica em `shared/` e `config/`.

Princípios seguidos (DDD + Clean Architecture, de forma pragmática):

- **Regra de dependência**: o domínio (entidades, regras) não conhece detalhes de
  infraestrutura; storage, gateway de pagamento e parsers são acessados por **portas**
  (interfaces) com **adaptadores** plugáveis.
- **Strategy** onde há variação real por tipo: parsers de arquivo (`DocumentParser`),
  exportadores de layout (`LayoutExporter`), gateways de pagamento (`PaymentGateway`),
  canais de notificação (`NotificationChannel`).
- **Eventos de domínio** desacoplam as etapas do pipeline e as integrações
  (conciliação, notificações, webhooks, métricas reagem a eventos, sem acoplamento
  direto).
- **Multiempresa desde o início**: toda entidade de negócio carrega `empresa_id`
  (`TenantEntity`), e o isolamento por empresa é aplicado em todas as consultas.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / framework | Java 21, Spring Boot 3.3 |
| Segurança | Spring Security, JWT (jjwt), BCrypt |
| Persistência | Spring Data JPA, PostgreSQL, Flyway (migrations) |
| Cache / fila | Redis, RabbitMQ (starters prontos; pipeline síncrono via `@Async` no MVP) |
| Storage | AWS S3 SDK apontando para MinIO (local) / Cloudflare R2 (produção) |
| OCR / parsing | Apache PDFBox, Tesseract (tess4j), Apache POI, Commons CSV |
| Mapeamento | MapStruct |
| Boilerplate | Lombok |
| Documentação | springdoc-openapi (Swagger UI) |
| Observabilidade | Actuator, Micrometer + Prometheus, logs JSON (logback) |
| Rate limiting | Bucket4j |
| Testes | JUnit 5, Mockito, Testcontainers |

---

## Estrutura de pastas

```
src/main/java/com/nalitech/
  config/            # Security, OpenAPI, Storage, JPA auditing, properties, seed inicial
  security/          # JWT (service, filtro), usuário autenticado, rate limiting
  shared/            # domínio base (entidades auditáveis), exceções, storage, utils, validação, cripto
  modules/
    auth/            # E1  login, refresh, reset de senha
    user/            # E1  usuários, perfis
    company/         # E2  empresas (CNPJ)
    client/          # E3  clientes do escritório
    file/            # E4  upload, storage, pipeline
    ocr/             # E5  extração de texto (PDFBox/Tesseract)
    parser/          # E6  parsers CSV/Excel/OFX/XML/TXT/PDF (Strategy)
    movement/        # E7  normalização de movimentações
    reconciliation/  # E8  conciliação bancária em camadas
    account/         # E9  plano de contas, motor de regras, aprendizado
    layout/          # E10 exportação por sistema contábil (Strategy)
    dashboard/       # E11 agregações da tela inicial
    finance/         # E12 honorários, cobranças (PIX/boleto), webhook do gateway
    fiscal/          # E13 agenda fiscal
    notification/    # E14 notificações (e-mail nativo; WhatsApp/push como stub → n8n)
    audit/           # E15 auditoria (via @Aspect)
    observability/   # E17 métricas customizadas
    webhook/         # E20 webhooks de saída (n8n)
    apikey/          # E20 API keys (entrada de automações)
    portal/          # E18 portal do cliente
src/main/resources/
  application.yml
  db/migration/      # V1..V15 (Flyway)
  logback-spring.xml
```

---

## O pipeline de processamento

Quando um arquivo é enviado (`POST /uploads`), o `UploadService` valida tipo/tamanho,
calcula o **SHA-256** (evita reprocessar duplicado), guarda no storage, grava o
registro e publica `ArquivoRecebidoEvent`. O `UploadPipelineService` reage ao evento
de forma assíncrona e conduz as etapas, atualizando o status do upload
(`RECEBIDO → VALIDANDO → PROCESSANDO → CONCLUIDO`/`ERRO`):

```
Upload → [OCR se PDF/imagem] → Parser (por formato) → Normalização → Conciliação → Classificação → Exportação
   E4            E5                   E6                   E7             E8            E9            E10
```

Cada etapa relevante emite um evento de domínio, que alimenta notificações (E14),
métricas (E17) e webhooks externos (E20) sem acoplamento.

A **conciliação** (E8) usa um algoritmo em camadas: (1) match exato por data+valor,
(2) valor + similaridade de descrição (Levenshtein), (3) regras configuráveis,
(4) pendente para revisão manual. As decisões do contador retroalimentam o
**aprendizado** (E9), melhorando as sugestões de conta futuras.

---

## Módulos e funcionalidades (épicos)

| Épico | Módulo | O que faz |
|---|---|---|
| E0 | config/security | Fundação: segurança JWT, CORS, tratamento de erro padrão, Flyway, Actuator, Swagger, Docker, rate limiting |
| E1 | auth/user | Login, refresh token revogável, reset de senha, CRUD de usuários e perfis (ADMIN/CONTADOR/AUXILIAR/CLIENTE) |
| E2 | company | Empresas com validação de CNPJ |
| E3 | client | Clientes do escritório com busca e documentos |
| E4 | file | Upload multipart, storage S3, hash anti-duplicidade, pipeline por evento |
| E5 | ocr | Texto nativo de PDF (PDFBox) com fallback Tesseract; imagens via OCR |
| E6 | parser | Parsers CSV, Excel, OFX, XML, TXT e PDF (via OCR), selecionados por `DocumentParserFactory` |
| E7 | movement | Normalização (data, sinal do valor, limpeza de descrição) num modelo único |
| E8 | reconciliation | Conciliação em camadas + confirmação/rejeição manual |
| E9 | account | Plano de contas, motor de regras, sugestão automática e histórico de aprendizado |
| E10 | layout | Exportação para Domínio, Alterdata, SCI, Questor e Custom (Strategy) |
| E11 | dashboard | Resumo (pendências, uploads do dia, erros, conciliações) e atividade recente |
| E12 | finance | Honorários e cobranças PIX/boleto via `PaymentGateway`; webhook do gateway idempotente |
| E13 | fiscal | Agenda fiscal com próximos vencimentos |
| E14 | notification | Notificações por e-mail (nativo); WhatsApp/push como stub (delegados ao n8n) |
| E15 | audit | Log de auditoria automático via `@Aspect` (login, exclusão, exportação) |
| E16 | (transversal) | Rate limiting, CSRF desabilitado (API stateless), `@PreAuthorize`, criptografia de dado sensível em repouso, LGPD |
| E17 | observability | Métricas Prometheus customizadas, logs JSON |
| E18 | portal | Portal do cliente (upload e status isolados por `cliente_id`) |
| E19 | (transversal) | Multiempresa já garantido via `empresa_id`; base para SaaS |
| E20 | webhook/apikey | Webhooks de saída assinados (HMAC) com retentativa + API keys com escopo |

---

## Segurança

- **JWT** stateless (access + refresh). O access token carrega `empresaId`/`clienteId`,
  base do isolamento multiempresa.
- **Autorização por perfil** com `@PreAuthorize` em cada endpoint.
- **API keys** (`X-API-Key`) como autenticação alternativa para automações, com escopos.
- **Rate limiting** nos endpoints públicos de autenticação.
- **Criptografia em repouso** (AES-GCM) de dado pessoal sensível (ex: telefone do cliente).
- **Webhooks** (entrada do gateway e saída para o n8n) validados/assinados por HMAC-SHA256.
- Segredos **sempre via variáveis de ambiente** — nunca commitados (ver `.env.example`).

---

## Integração externa (n8n)

Em vez de implementar cada canal (WhatsApp, planilhas, CRM) dentro do backend, o
Nalitech expõe um **padrão único**:

- **Saída** — `webhook_subscriptions`: o n8n assina eventos (`upload.processado`,
  `conciliacao.pendente`, `cobranca.paga`, `obrigacao.vencendo`, ...). Cada entrega é
  assinada (`X-Nalitech-Signature`) e registrada em `webhook_deliveries`, com
  retentativa e backoff.
- **Entrada** — `api_keys`: o n8n chama a API (ex: `POST /uploads`) usando `X-API-Key`.

---

## Como rodar

> Requer **JDK 21** e **Maven**. As dependências de infra (Postgres, Redis, MinIO,
> RabbitMQ) sobem via Docker.

1. **Suba a infra local** (Postgres/Redis/MinIO/RabbitMQ). O `01-infra.md` traz um
   `docker-compose.yml` de referência — como este repositório é só o backend, aponte o
   build/serviço `backend` para esta pasta (ou rode o backend direto pelo Maven).

2. **Configure o ambiente**: copie `.env.example` para `.env` e ajuste se necessário.
   Os valores padrão já funcionam para desenvolvimento local. Exporte-os antes de rodar
   (ex: `set -a && source .env && set +a`), ou defina como variáveis do sistema.

3. **Rode as migrations + a aplicação**:
   ```bash
   mvn spring-boot:run
   ```
   O Flyway cria o schema (V1..V15). Na primeira subida, um usuário **ADMIN inicial** é
   criado (`DEFAULT_ADMIN_EMAIL` / `DEFAULT_ADMIN_PASSWORD`).

4. **Acesse**:
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Health: `http://localhost:8080/actuator/health`
   - Métricas: `http://localhost:8080/actuator/prometheus`

5. **Build do jar / imagem Docker**:
   ```bash
   mvn clean package
   docker build -t nalitech-backend .
   ```

### Fluxo rápido de uso

```
POST /auth/login            → obtém access + refresh token
POST /companies             → (ADMIN) cadastra empresa
POST /clients               → cadastra cliente
POST /uploads (multipart)   → envia extrato/planilha → dispara o pipeline
GET  /reconciliations/pending → revisa conciliações
POST /reconciliations/{id}/confirm
GET  /movements/{id}/suggestions → sugestão de conta
POST /movements/{id}/classify
POST /layouts/DOMINIO/export?inicio=...&fim=... → gera arquivo de exportação
```

---

## Testes

Testes unitários com JUnit 5 + Mockito cobrindo pontos críticos:

- `AuthServiceTest` — login válido/ inválido / inexistente.
- `CnpjValidatorTest` — dígito verificador.
- `ParsersTest` — CSV e OFX.
- `StringSimilarityTest` — similaridade de descrição (conciliação).
- `PaymentWebhookServiceTest` — idempotência e validação de assinatura.
- `HmacSignerTest` — assinatura dos webhooks.

```bash
mvn test
```

Testes de integração que dependem de Postgres usam **Testcontainers** (as dependências
já estão no `pom.xml`).

---

## Observabilidade

- `/actuator/health`, `/actuator/info`, `/actuator/prometheus`.
- Métricas customizadas: `nalitech.uploads.processados`, `nalitech.uploads.erro`,
  `nalitech.movimentacoes.geradas`.
- Logs em JSON ativando o profile `json` (`SPRING_PROFILES_ACTIVE=json`).

---

## Notas e limitações

- **Processamento síncrono no MVP**: o pipeline roda via `@Async` (não via RabbitMQ),
  conforme a decisão de infraestrutura para um único escritório. Os starters de fila já
  estão prontos para migrar quando o volume justificar.
- **OCR (Tesseract)**: exige o binário/tessdata no ambiente. Sem ele, a extração de PDF
  escaneado/imagem degrada com confiança baixa, sem quebrar o pipeline.
- **Gateway de pagamento**: o provedor padrão é `SIMULADO` (gera dados de cobrança
  falsos para exercitar o fluxo). Há um adaptador `ASAAS` (estrutura de chamada) a
  validar com a documentação do provedor no momento da integração.
- **Layouts de exportação**: os formatos (Domínio/Alterdata/SCI/Questor) são bases
  plausíveis — o layout exato deve ser validado com a contadora.
- **Schema**: o Flyway é a fonte única do schema; o Hibernate roda em modo `validate`
  (nunca altera tabelas).
```
