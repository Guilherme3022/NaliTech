package com.nalitech.modules.layout.exporter;

import com.nalitech.modules.movement.entity.Movement;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AlterdataExporter extends AbstractLayoutExporter {

    @Override
    public String sistema() {
        return "ALTERDATA";
    }

    @Override
    public ExportedFile export(List<Movement> movements) {
        String header = "DATA;VALOR;TIPO;HISTORICO;DOCUMENTO";
        String body = movements.stream()
                .map(m -> String.join(";", data(m), valor(m), tipo(m), descricao(m), documento(m)))
                .collect(Collectors.joining("\n"));
        return text("export-alterdata.csv", header + "\n" + body);
    }
}
