package com.nalitech.modules.layout.exporter;

import com.nalitech.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LayoutExporterFactory {

    private final List<LayoutExporter> exporters;

    public LayoutExporterFactory(List<LayoutExporter> exporters) {
        this.exporters = exporters;
    }

    public LayoutExporter resolve(String sistema) {
        return exporters.stream()
                .filter(exporter -> exporter.supports(sistema))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Sistema de exportacao nao suportado: " + sistema,
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    public List<String> sistemasSuportados() {
        return exporters.stream().map(LayoutExporter::sistema).sorted().toList();
    }
}
