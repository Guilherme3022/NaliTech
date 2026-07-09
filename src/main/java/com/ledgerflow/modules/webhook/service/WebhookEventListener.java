package com.ledgerflow.modules.webhook.service;

import com.ledgerflow.modules.file.event.ArquivoRecebidoEvent;
import com.ledgerflow.modules.file.event.UploadErroEvent;
import com.ledgerflow.modules.file.event.UploadProcessadoEvent;
import com.ledgerflow.modules.finance.event.InvoiceEvents.InvoiceCreatedEvent;
import com.ledgerflow.modules.finance.event.InvoiceEvents.InvoiceOverdueEvent;
import com.ledgerflow.modules.finance.event.InvoiceEvents.InvoicePaidEvent;
import com.ledgerflow.modules.fiscal.event.ObrigacaoVencendoEvent;
import com.ledgerflow.modules.layout.event.ExportacaoGeradaEvent;
import com.ledgerflow.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoConfirmadaEvent;
import com.ledgerflow.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoPendenteEvent;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventListener {

    private final WebhookDispatcherService dispatcher;

    public WebhookEventListener(WebhookDispatcherService dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventListener
    public void onUploadRecebido(ArquivoRecebidoEvent e) {
        dispatcher.dispatch(e.empresaId(), "upload.recebido", data(
                "upload_id", e.uploadId(), "cliente_id", e.clienteId(), "tipo", e.tipoMime()));
    }

    @EventListener
    public void onUploadProcessado(UploadProcessadoEvent e) {
        dispatcher.dispatch(e.empresaId(), "upload.processado", data(
                "upload_id", e.uploadId(), "quantidade", e.quantidadeMovimentacoes()));
    }

    @EventListener
    public void onUploadErro(UploadErroEvent e) {
        dispatcher.dispatch(e.empresaId(), "upload.erro", data(
                "upload_id", e.uploadId(), "etapa", e.etapa(), "mensagem", e.mensagem()));
    }

    @EventListener
    public void onConciliacaoPendente(ConciliacaoPendenteEvent e) {
        dispatcher.dispatch(e.empresaId(), "conciliacao.pendente", data(
                "movement_id", e.movementId(), "motivo", e.motivo()));
    }

    @EventListener
    public void onConciliacaoConfirmada(ConciliacaoConfirmadaEvent e) {
        dispatcher.dispatch(e.empresaId(), "conciliacao.confirmada", data(
                "movement_id", e.movementId(), "conta", e.contaSugerida()));
    }

    @EventListener
    public void onExportacaoGerada(ExportacaoGeradaEvent e) {
        dispatcher.dispatch(e.empresaId(), "exportacao.gerada", data(
                "sistema", e.sistema(), "periodo_inicio", e.periodoInicio(),
                "periodo_fim", e.periodoFim(), "file_id", e.fileId()));
    }

    @EventListener
    public void onObrigacaoVencendo(ObrigacaoVencendoEvent e) {
        dispatcher.dispatch(e.empresaId(), "obrigacao.vencendo", data(
                "cliente_id", e.clienteId(), "tipo", e.tipo(), "vencimento", e.vencimento()));
    }

    @EventListener
    public void onCobrancaCriada(InvoiceCreatedEvent e) {
        dispatcher.dispatch(e.empresaId(), "cobranca.criada", data(
                "cliente_id", e.clienteId(), "valor", e.valor(), "boleto_url", e.boletoUrl()));
    }

    @EventListener
    public void onCobrancaPaga(InvoicePaidEvent e) {
        dispatcher.dispatch(e.empresaId(), "cobranca.paga", data(
                "cliente_id", e.clienteId(), "valor", e.valor()));
    }

    @EventListener
    public void onCobrancaVencida(InvoiceOverdueEvent e) {
        dispatcher.dispatch(e.empresaId(), "cobranca.vencida", data(
                "cliente_id", e.clienteId(), "valor", e.valor(), "dias_atraso", e.diasEmAtraso()));
    }

    private Map<String, Object> data(Object... pairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
