# Segurança — Nalitech

Este documento resume **o que já está protegido** no Nalitech e o **checklist do
que falta** para levar o sistema a produção com segurança. A ideia é ser um guia
prático, não um tratado.

> Resumo em uma frase: **o esqueleto é seguro por design** (autenticação,
> autorização, isolamento por empresa, criptografia, rate limit), mas "seguro
> contra invasão de verdade" depende de **higiene de segredos + TLS + hardening
> de borda + varredura de dependências** — coisas de deploy/operação.

---

## 1. O que já está implementado (base sólida)

| Área | Como está | Onde |
|---|---|---|
| **Autenticação** | JWT stateless assinado com HMAC-SHA; sessão `STATELESS` | `security/JwtService`, `SecurityConfig` |
| **Senhas** | Hash com **BCrypt** (salt automático) | `SecurityConfig#passwordEncoder` |
| **Autorização** | `@EnableMethodSecurity` + `@PreAuthorize` por endpoint (papéis) | controllers |
| **Isolamento multi-empresa** | Consultas filtram por `empresaId` do usuário logado | `SecurityUtils.currentEmpresaId()` |
| **Força bruta** | Rate limit nos endpoints públicos (login/reset) | `security/RateLimitFilter` |
| **Integrações** | Autenticação por **API Key** | `apikey/security/ApiKeyAuthenticationFilter` |
| **CORS** | Restrito a origens configuradas (não usa `*`) | `SecurityConfig#corsConfigurationSource` |
| **Dados sensíveis** | Criptografia de atributos no banco (AES) | `shared/security/AttributeEncryptor` |
| **Webhooks** | Validação de assinatura HMAC | `PaymentWebhookService`, `HmacSigner` |
| **Segredos** | Lidos de variáveis de ambiente; `.env` no `.gitignore` | `application.yml` |
| **Auditoria** | Trilha de ações relevantes | módulo `audit` |
| **Validação de entrada** | `@Valid` nos DTOs; acesso a dados via JPA | controllers/repos |

---

## 2. Checklist para produção (o que falta endurecer)

### 🔴 Crítico — fazer antes de qualquer deploy real

- [ ] **Trocar TODOS os segredos default.** Hoje há valores de desenvolvimento
      no `application.yml`/`docker-compose`:
  - `JWT_SECRET` (default "troque-isso...") → gerar segredo forte (≥ 32 bytes).
  - `NALITECH_ENCRYPTION_KEY` (default `0123...`) → chave forte e única.
  - `DB_PASSWORD`, `RABBITMQ_PASSWORD`, `STORAGE_SECRET_KEY` → senhas fortes.
  - `DEFAULT_ADMIN_*` → trocar a senha do admin inicial no primeiro acesso.
- [ ] **Usar um cofre de segredos** em produção (AWS Secrets Manager, Vault,
      variáveis do provedor de deploy) — nunca commitar segredo.
- [ ] **HTTPS/TLS obrigatório** na borda (proxy/ingress) + redirecionar HTTP→HTTPS.

### 🟠 Importante — logo em seguida

- [ ] **Cabeçalhos de segurança + CSP** no nginx do frontend (HSTS,
      `X-Content-Type-Options`, `X-Frame-Options`/frame-ancestors, CSP). Como o
      SPA guarda o JWT no cliente, um XSS pode roubar o token — CSP reduz esse risco.
- [ ] **Rate limit distribuído.** Hoje é em memória (`ConcurrentHashMap`, por
      instância) — com múltiplas réplicas ele não é global. Migrar para Redis.
- [ ] **Varredura de dependências no CI**: OWASP Dependency-Check (Java) e
      `npm audit`/Dependabot (frontend) para pegar CVEs de bibliotecas.
- [ ] **Revisar exposição do Actuator**: só `/actuator/health` é público (ok);
      garantir que os demais endpoints continuem exigindo autenticação.

### 🟡 Recomendado — maturidade

- [ ] **Revogação/rotação de token**: como o JWT é stateless, o logout não
      invalida o token até expirar. Avaliar blacklist (Redis) e rotação de
      refresh token.
- [ ] **Política de senhas** (tamanho mínimo, complexidade) e **bloqueio por
      tentativas** por usuário, além do rate limit por IP.
- [ ] **Logs sem dados sensíveis**: garantir que tokens, senhas e PII não vão
      para o log (revisar MDC/mensagens).
- [ ] **Backups do banco + teste de restauração** e retenção definida.
- [ ] **2FA/MFA** para papéis administrativos (evolução futura).
- [ ] **Testes de segurança**: incluir casos de autorização (um usuário não
      acessar dados de outra empresa) e, se possível, um scan DAST (ex.: OWASP ZAP).

---

## 3. Notas de design (por que algumas escolhas)

- **CSRF desabilitado**: correto para uma API **stateless** consumida por SPA/apps
  via `Authorization: Bearer`. CSRF é problema de sessão baseada em cookie; aqui
  a defesa relevante é contra **XSS** (ver CSP acima).
- **CORS com `allowCredentials(true)`**: exige lista de origens explícita (não
  `*`) — é o que o código faz. Mantenha `CORS_ALLOWED_ORIGINS` restrito aos
  domínios reais do frontend.
- **Isolamento por empresa**: é a defesa central de multi-tenant. Qualquer novo
  endpoint/consulta **deve** filtrar por `empresaId` — trate isso como regra
  obrigatória em code review.

---

## 4. Variáveis de ambiente sensíveis (referência)

| Variável | Papel | Em produção |
|---|---|---|
| `JWT_SECRET` | Assinatura dos tokens JWT | segredo forte, único |
| `NALITECH_ENCRYPTION_KEY` | Criptografia de atributos no banco | chave forte, rotacionável |
| `PAYMENT_WEBHOOK_SECRET` | Validação de webhooks de pagamento | preencher (ver `PAGAMENTOS.md`) |
| `DB_PASSWORD` / `RABBITMQ_PASSWORD` / `STORAGE_SECRET_KEY` | Credenciais de infra | senhas fortes |
| `CORS_ALLOWED_ORIGINS` | Origens liberadas no CORS | só domínios reais do front |

> Nunca versione esses valores. Eles vivem no `.env` (ignorado pelo Git) ou no
> cofre de segredos do ambiente de deploy.
