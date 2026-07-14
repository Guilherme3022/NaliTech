# NALI — Guia de Funcionalidades

> Documento didático: explica **para que serve cada parte** do sistema.
> Não é manual de deploy (isso está em `DEPLOY.md`) nem roadmap técnico
> (isso está em `02-backend.md`). Aqui o foco é entender o **produto**.

---

## 1. O que é o NALI

O NALI é uma plataforma que **automatiza a conciliação contábil** de escritórios
de contabilidade. Em vez de o contador classificar manualmente cada linha de um
extrato bancário, o sistema:

1. recebe os arquivos (extratos, planilhas, etc.);
2. interpreta as movimentações;
3. sugere em qual **conta contábil** cada movimentação entra (por regras ou IA);
4. permite revisar e concluir;
5. gera o arquivo final para importar no sistema contábil.

---

## 2. Conceitos-chave (a hierarquia)

A espinha dorsal do sistema é esta cadeia:

```
Empresa → Cliente → Plano de Contas → Perfil de Conciliação → Importações → Conciliações → Exportações
```

| Conceito | O que é | Exemplo |
|---|---|---|
| **Empresa** | O escritório de contabilidade que usa a plataforma (é o "cliente interno" do NALI). | Escritório Contábil Alfa |
| **Cliente** | Cada empresa atendida pelo escritório (o "cliente do seu cliente"). | Mercado Sol Ltda. |
| **Plano de contas** | O catálogo de contas contábeis **de cada cliente** — onde cada movimentação pode ser registrada. | 1.1.01 Caixa, 4.2 Despesas Bancárias |
| **De/Para (regra)** | A "tradução": quando uma descrição bate com um padrão, usa tal conta. | "Se contém TARIFA → 4.2 Despesas Bancárias" |
| **Perfil de Conciliação** | Um "pacote de configuração" por cliente: sistema de origem, tipo de arquivo, sistema contábil de destino e plano. | "Movimentação Financeira – Domínio" |
| **Competência** | O período contábil (mês/ano) do processamento. | Junho/2026 |
| **Conciliação (lote)** | O processo mensal de um cliente: junta arquivos + regras + resultado + situação. | "Conciliação 000125 – Mercado Sol – Jun/2026" |
| **Exportação** | O arquivo final gerado a partir de uma conciliação concluída. | `conciliacao-ab12cd34.txt` |

> **Importante:** tudo é isolado por empresa. Um escritório **nunca** enxerga
> dados de outro. E, dentro de um escritório, você trabalha sempre no contexto
> de **um cliente selecionado**.

---

## 3. Papéis de usuário (quem pode o quê)

| Papel | Quem é | O que faz |
|---|---|---|
| **ADMIN geral** | Dono da plataforma (você). Fica "acima" das empresas. | Único que **transita entre empresas**. Gerencia empresas, usuários, configurações. |
| **CONTADOR** | Contador de um escritório. Preso à sua empresa. | Alterna entre os **clientes** da empresa; faz importações, parametrização e conciliações. |
| **AUXILIAR** | Auxiliar do escritório. Preso à sua empresa. | Opera importações/conciliações (acesso mais restrito). |
| **CLIENTE** | O cliente final do escritório. | Acessa um **portal simplificado** (envia documentos, acompanha). |

**Regra de ouro:** só o ADMIN geral pode existir sem empresa. Qualquer usuário
comum **obrigatoriamente** pertence a uma empresa.

---

## 4. O cabeçalho (seletores do topo)

No topo da tela ficam os seletores que definem o **contexto de trabalho**:

- **Empresa** (só ADMIN): escolhe em qual escritório está trabalhando. Ao trocar,
  todos os dados da tela recarregam no contexto da nova empresa.
- **Cliente**: escolhe qual cliente do escritório está sendo tratado. Trocar o
  cliente atualiza plano de contas, importações, conciliações, etc.
- **Competência**: o mês/ano do trabalho.

> Pense nesses três como um "filtro global": quase tudo que você vê depende de
> qual empresa + cliente + competência estão selecionados.

---

## 5. Menu por menu — para que serve cada tela

### Dashboard
Visão geral com números do escritório (pendências, uploads recentes, etc.).

### Clientes
Cadastro dos clientes do escritório (as empresas atendidas). Cada cliente terá
seu plano de contas, arquivos e conciliações.

### Movimentações
Os **lançamentos extraídos dos arquivos** (cada linha de um extrato vira uma
movimentação: data, valor, descrição, conta, status). Aqui você **vê, edita e
exclui** — útil para corrigir uma leitura torta (ex.: valor errado num PDF) ou
remover uma duplicata, sem precisar reenviar o arquivo. Filtra por cliente e
competência selecionados no topo.

> **Não existe mais um menu "Uploads".** O envio de arquivos acontece **dentro da
> conciliação** (você abre o lote e envia ali). O status/erro de processamento de
> cada arquivo aparece no próprio detalhe da conciliação, e um clique em "Detalhe"
> abre a tela de processamento do arquivo (para depurar erros).

### Conciliação
O coração do sistema. Tem duas visões:
- **Conciliações do cliente (cards):** cada card é um **lote/processo mensal**
  (ex.: Mercado Sol – Jun/2026), com sua situação (Rascunho, Concluída...).
  Você cria uma nova, abre o detalhe, conclui, cancela e baixa o resultado.
- **Itens (Pendentes / Histórico):** a conferência linha a linha (extrato ×
  sistema), onde você confirma ou rejeita cada movimentação.

> Só é possível **confirmar/concluir** se o cliente tiver um **plano de contas
> ativo**. Sem plano, o sistema bloqueia e avisa.

### Detalhe da conciliação (ao abrir um card)
É aqui que a conciliação acontece de ponta a ponta. Mostra a situação do lote e permite:
- **Enviar o arquivo direto na conciliação** (faz o upload já vinculado ao
  cliente do lote **e anexa automaticamente** — um passo só);
- **Anexar** um arquivo já existente do cliente ao lote;
- **Substituir** um arquivo (cria nova versão, mantém histórico);
- **Concluir** (exige plano + ao menos um arquivo) ou **Cancelar**;
- **Baixar** o resultado em TXT/CSV (só quando concluída).

> Ou seja: você **não precisa** ir à tela de Uploads separadamente para
> conciliar — abre o lote e envia o arquivo ali dentro. A tela **Uploads**
> continua existindo para gestão geral de arquivos, mas o fluxo de conciliação
> é autocontido aqui.

### Plano de contas
O catálogo de contas de cada cliente. É o que diz "onde cada movimentação será
registrada". Pode ter contas específicas do cliente ou contas compartilhadas do
escritório. Você pode montar as contas **manualmente**, aplicar um **plano-modelo**,
ou **importar de um Excel/CSV** (botão "Importar plano") — o arquivo precisa ter
colunas `codigo` e `nome` (e opcionalmente `tipo`); códigos já existentes são
ignorados. (Importar de PDF não é confiável; use Excel/CSV.)

### Perfis de conciliação
Um "pacote de configuração" por cliente: de onde vêm os dados (sistema de
origem), tipo de arquivo, para qual sistema contábil exporta e qual plano usa.
Evita reconfigurar tudo a cada mês. **Exige que o cliente já tenha plano.** Ao
criar uma nova conciliação, você pode **escolher o perfil** — ele fica vinculado
ao lote (aparece no card).

### Planos-modelo
Estruturas de plano de contas **reutilizáveis, por escritório**. Você monta um
modelo uma vez (ex.: "Plano Padrão Comércio") e **aplica a vários clientes** —
o sistema copia as contas para o plano de cada cliente. Cada cliente mantém sua
cópia independente (pode customizar depois).

### Layouts de importação
Define **como ler cada arquivo** (quais colunas são data, valor, descrição...).
É o que permite o sistema entender formatos diferentes de extrato/planilha.

### Exportação
Histórico/central de exportações (mais administrativo). No dia a dia, o download
do resultado já fica **dentro da própria conciliação**.

### Financeiro
Honorários e cobranças do escritório com seus clientes.

### Agenda fiscal
Obrigações e prazos fiscais.

### Empresas *(só ADMIN)*
Cadastro dos escritórios (clientes internos da plataforma). Só o dono do NALI
gerencia.

### Usuários *(só ADMIN)*
Cadastro dos usuários. Usuário comum precisa de empresa; ADMIN pode ser global.

### Auditoria *(só ADMIN)*
Registro de ações no sistema (quem fez o quê, quando). Retenção prevista ~1 ano.

### Configurações *(só ADMIN)*
Webhooks e chaves de API (integrações, ex.: n8n).

---

## 6. Fluxo mensal completo (o "caminho feliz")

```
1. Selecionar Empresa (ADMIN) e Cliente no topo
2. Garantir que o cliente tem Plano de Contas
   (criar do zero, ou aplicar um Plano-modelo)
3. (Opcional) Criar/escolher um Perfil de Conciliação
4. Selecionar a Competência (mês)
5. Em Conciliação: criar o lote (card) da competência
6. Abrir o lote e enviar o arquivo ali mesmo (upload + anexo num passo)
7. Revisar os itens (confirmar/parametrizar pendências)
8. Concluir a conciliação
9. Baixar o resultado (TXT/CSV) para o sistema contábil
```

> O envio do arquivo virou parte da própria conciliação — não é mais um passo
> separado na tela de Uploads.

---

## 7. Regras importantes (invariantes)

Essas regras o sistema **garante** (bloqueia quando violadas):

1. Nenhum usuário comum sem empresa (só ADMIN é global).
2. Nenhum cliente sem empresa.
3. Nenhum arquivo enviado sem cliente.
4. Nenhuma conciliação sem cliente.
5. Nenhuma conciliação concluída sem **plano de contas ativo** do cliente.
6. Nenhuma conciliação concluída sem **ao menos um arquivo** anexado.
7. Download só depois de concluir a conciliação.
8. Arquivo de conciliação concluída **não é excluído** — só substituído (mantém
   histórico e rastreabilidade).
9. Usuário comum só vê dados da própria empresa; ADMIN alterna entre empresas;
   usuários alternam entre clientes da sua empresa.
10. Código de conta é único **por cliente** (clientes diferentes podem repetir
    a mesma estrutura de códigos).

---

## 8. Como o NALI "adivinha" a conta (parametrização)

Quando chega uma movimentação, a ordem de classificação é:

```
1. Regra explícita (De/Para)  →  2. IA (se ligada)  →  3. Histórico  →  4. Manual
```

- **Regra De/Para:** "se a descrição contém X, use a conta Y". Determinística.
- **IA (opcional):** um modelo de linguagem sugere a conta (ver `DEPLOY.md` §5).
  Se desligada ou se falhar, cai na heurística — nunca quebra o fluxo.
- **Histórico:** aprende com decisões anteriores do contador.
- **Manual:** o contador escolhe, e pode **salvar como nova regra** para o futuro.

Quanto mais o escritório parametriza, mais o sistema resolve sozinho.
