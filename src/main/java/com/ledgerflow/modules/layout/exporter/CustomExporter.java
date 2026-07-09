package com.ledgerflow.modules.layout.exporter;

import com.ledgerflow.modules.movement.entity.Movement;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CustomExporter extends AbstractLayoutExporter {

    @Override
    public String sistema() {
        return "CUSTOM";
    }

    @Override
    public ExportedFile export(List<Movement> movements) {
        String header = "data,valor,tipo,descricao,documento";
        String body = movements.stream()
                .map(m -> String.join(",", data(m), valor(m), tipo(m),
                        escape(descricao(m)), documento(m)))
                .collect(Collectors.joining("\n"));
        return new ExportedFile("export-custom.csv", "text/csv",
                (header + "\n" + body).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
