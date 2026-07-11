package com.nalitech.modules.observability;

import com.nalitech.modules.file.event.UploadErroEvent;
import com.nalitech.modules.file.event.UploadProcessadoEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ObservabilityMetrics {

    private final Counter uploadsProcessados;
    private final Counter uploadsComErro;
    private final Counter movimentacoesGeradas;

    public ObservabilityMetrics(MeterRegistry registry) {
        this.uploadsProcessados = Counter.builder("nalitech.uploads.processados")
                .description("Total de uploads processados com sucesso")
                .register(registry);
        this.uploadsComErro = Counter.builder("nalitech.uploads.erro")
                .description("Total de uploads que falharam no pipeline")
                .register(registry);
        this.movimentacoesGeradas = Counter.builder("nalitech.movimentacoes.geradas")
                .description("Total de movimentacoes normalizadas")
                .register(registry);
    }

    @EventListener
    public void onUploadProcessado(UploadProcessadoEvent event) {
        uploadsProcessados.increment();
        movimentacoesGeradas.increment(event.quantidadeMovimentacoes());
    }

    @EventListener
    public void onUploadErro(UploadErroEvent event) {
        uploadsComErro.increment();
    }
}
