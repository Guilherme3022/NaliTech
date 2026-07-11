package com.nalitech.modules.layout.exporter;

import com.nalitech.modules.movement.entity.Movement;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DominioExporter extends AbstractLayoutExporter {

    @Override
    public String sistema() {
        return "DOMINIO";
    }

    @Override
    public ExportedFile export(List<Movement> movements) {
        String content = movements.stream()
                .map(m -> String.join("|", data(m), valor(m), tipo(m), descricao(m), documento(m)))
                .collect(Collectors.joining("\n"));
        return text("export-dominio.txt", content);
    }
}
