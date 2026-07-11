package com.nalitech.modules.layout.exporter;

import com.nalitech.modules.movement.entity.Movement;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SciExporter extends AbstractLayoutExporter {

    @Override
    public String sistema() {
        return "SCI";
    }

    @Override
    public ExportedFile export(List<Movement> movements) {
        String content = movements.stream()
                .map(m -> pad(data(m), 10) + pad(valor(m), 15) + pad(descricao(m), 40))
                .collect(Collectors.joining("\n"));
        return text("export-sci.txt", content);
    }

    private String pad(String value, int width) {
        String safe = value == null ? "" : value;
        if (safe.length() >= width) {
            return safe.substring(0, width);
        }
        return safe + " ".repeat(width - safe.length());
    }
}
