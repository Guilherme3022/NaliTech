# Pagamentos — Guia de Configuração

Este documento explica como o módulo financeiro (cobranças de mensalidade do
escritório) funciona e **o que precisa ser configurado quando você decidir
ligar a cobrança de verdade** (hoje ele roda em modo simulado).

> Enquanto você não configurar um provedor real, **nada é cobrado**: o sistema
> gera cobranças "de mentira" (PIX/boleto falsos) apenas para você testar o
> fluxo de ponta a ponta.

---

## 1. Como está hoje (modo simulado)

O provedor de pagamento é escolhido pela variável de ambiente
`PAYMENT_PROVIDER`. O valor padrão é `SIMULADO`:

```env
PAYMENT_PROVIDER=SIMULADO
```

Com `SIMULADO`, a implementação usada é `SimulatedPaymentGateway`. Ela:

- gera um `externalId` fake (`sim_<uuid>`);
- devolve URLs de boleto e um "PIX copia e cola" fictícios;
- **não** chama nenhuma API externa e **não** movimenta dinheiro.

Isso permite exercitar toda a jornada (criar fatura → gerar cobrança →
receber webhook → marcar como paga) sem depender de um provedor real.

---

## 2. Provedor real disponível: Asaas

Já existe uma implementação pronta para o **Asaas** (`AsaasPaymentGateway`),
apontando por padrão para o ambiente **sandbox**. Para ativá-la, basta trocar
o provedor e informar o token.

### 2.1. Variáveis de ambiente

Adicione (ou descomente) no seu `.env` do backend:

```env
# Liga o provedor real
PAYMENT_PROVIDER=ASAAS

# Token de API do Asaas (NUNCA versionar / commitar)
ASAAS_API_TOKEN=coloque_o_token_aqui

# URL base da API:
#   Sandbox (testes):  https://sandbox.asaas.com/api/v3   (padrão)
#   Produção (real):   https://api.asaas.com/v3
ASAAS_BASE_URL=https://sandbox.asaas.com/api/v3

# Segredo para validar a assinatura dos webhooks (ver seção 3)
PAYMENT_WEBHOOK_SECRET=um_segredo_forte_aqui
```

> Referência no código:
> - `PaymentGatewayFactory` lê `PAYMENT_PROVIDER` para escolher o gateway.
> - `AsaasPaymentGateway` lê `ASAAS_BASE_URL` e `ASAAS_API_TOKEN`.
> - `PaymentWebhookService` lê `PAYMENT_WEBHOOK_SECRET`.

### 2.2. Onde obter o token do Asaas

1. Crie uma conta no Asaas (comece pelo **sandbox** para testar):
   - Sandbox: <https://sandbox.asaas.com>
   - Produção: <https://www.asaas.com>
2. No painel, vá em **Configurações → Integrações → API** (ou "Chave de API").
3. Gere / copie a **API Key** e coloque em `ASAAS_API_TOKEN`.
4. Comece com a URL de **sandbox**; só troque para produção quando validar todo
   o fluxo.

### 2.3. Passagem de sandbox → produção

Ao migrar para produção, altere **duas** coisas:

- `ASAAS_BASE_URL=https://api.asaas.com/v3`
- `ASAAS_API_TOKEN=` → token da conta **de produção** (o token de sandbox não
  funciona em produção).

---

## 3. Webhooks (confirmação de pagamento)

O status da fatura (`PAGO`, `VENCIDO`) é atualizado quando o provedor envia um
**webhook** para o backend. Sem webhook configurado, a cobrança é criada mas o
sistema não sabe quando ela foi paga.

### 3.1. Endpoint que recebe o webhook

```
POST /webhooks/payments/{provider}
```

Exemplo para o Asaas (ajuste o domínio para o do seu backend em produção):

```
https://SEU-BACKEND/webhooks/payments/asaas
```

> `{provider}` é o mesmo nome usado para casar a fatura; use `asaas`.

### 3.2. Validação de assinatura

- Se `PAYMENT_WEBHOOK_SECRET` **estiver vazio**, a validação de assinatura é
  **pulada** (útil só em desenvolvimento).
- Se estiver preenchido, o backend valida o header **`X-Signature`** usando
  HMAC (`HmacSigner.matches`) sobre o corpo bruto da requisição.

> ⚠️ Importante: o Asaas, por padrão, autentica webhooks por um **token de
> acesso** (header próprio), não necessariamente por HMAC `X-Signature`. Ao ligar
> em produção, confira no painel do Asaas qual mecanismo de assinatura ele envia
> e ajuste a validação em `PaymentWebhookService.validateSignature` se
> necessário, para bater com o header/algoritmo real do provedor.

### 3.3. Configurando o webhook no Asaas

1. No painel do Asaas: **Configurações → Integrações → Webhooks** (ou
   "Notificações via API").
2. Cadastre a URL: `https://SEU-BACKEND/webhooks/payments/asaas`.
3. Marque os eventos de cobrança (ex.: pagamento confirmado / recebido /
   vencido).
4. Se o Asaas permitir configurar um segredo/token de assinatura, use o mesmo
   valor de `PAYMENT_WEBHOOK_SECRET`.

### 3.4. Eventos tratados

O `PaymentWebhookService` normaliza o tipo do evento e reage assim:

| Evento contém        | Ação na fatura                    |
|----------------------|-----------------------------------|
| `CONFIRM` / `RECEIVED` / `PAGO` | marca como **PAGO** e dispara `InvoicePaidEvent` |
| `OVERDUE` / `VENCID` | marca como **VENCIDO** e dispara `InvoiceOverdueEvent` |

Eventos duplicados (mesmo provider + externalId + tipo) são ignorados
(idempotência).

---

## 4. Checklist para "ligar" a cobrança de verdade

- [ ] Criar conta no Asaas (sandbox primeiro).
- [ ] Gerar a API Key e definir `ASAAS_API_TOKEN`.
- [ ] Definir `PAYMENT_PROVIDER=ASAAS`.
- [ ] Manter `ASAAS_BASE_URL` de **sandbox** para testar.
- [ ] Definir `PAYMENT_WEBHOOK_SECRET` e cadastrar o webhook no painel.
- [ ] Testar o fluxo completo (criar fatura → cobrar → pagar no sandbox →
      webhook → fatura vira `PAGO`).
- [ ] Conferir/ajustar a validação de assinatura conforme o header real do
      provedor.
- [ ] Só então trocar `ASAAS_BASE_URL` e `ASAAS_API_TOKEN` para **produção**.

---

## 5. Arquivos relevantes (para referência)

| Arquivo | Papel |
|---|---|
| `modules/finance/gateway/PaymentGateway.java` | Interface do provedor |
| `modules/finance/gateway/PaymentGatewayFactory.java` | Escolhe o provedor via `PAYMENT_PROVIDER` |
| `modules/finance/gateway/SimulatedPaymentGateway.java` | Provedor fake (padrão) |
| `modules/finance/gateway/AsaasPaymentGateway.java` | Integração real com o Asaas |
| `modules/finance/controller/PaymentWebhookController.java` | Recebe `POST /webhooks/payments/{provider}` |
| `modules/finance/service/PaymentWebhookService.java` | Valida assinatura, atualiza fatura, idempotência |
| `modules/finance/service/InvoiceService.java` | Regras de faturas/cobranças |

---

## 6. Segurança

- **Nunca** commite `ASAAS_API_TOKEN` nem `PAYMENT_WEBHOOK_SECRET`. Eles ficam
  só no `.env` (que está no `.gitignore`) ou nas variáveis do ambiente de
  deploy.
- Em produção, o endpoint de webhook deve estar atrás de HTTPS.
- Mantenha `PAYMENT_WEBHOOK_SECRET` preenchido em produção para não aceitar
  webhooks forjados.
