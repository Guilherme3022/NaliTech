package com.ledgerflow.modules.layout.exporter;

import com.ledgerflow.modules.movement.entity.Movement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

abstract class AbstractLayoutExporter implements LayoutExporter {

    protected static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean supports(String sistema) {
        return sistema().equalsIgnoreCase(sistema);
    }

    protected String data(Movement movement) {
        LocalDate data = movement.getData();
        return data == null ? "" : data.format(DATE_BR);
    }

    protected String valor(Movement movement) {
        BigDecimal valor = movement.getValor();
        if (valor == null) {
            return "0,00";
        }
        return valor.abs().toPlainString().replace(".", ",");
    }

    protected String tipo(Movement movement) {
        return movement.getTipo() == null ? "" : movement.getTipo().name();
    }

    protected String descricao(Movement movement) {
        return movement.getDescricao() == null ? "" : movement.getDescricao();
    }

    protected String documento(Movement movement) {
        return movement.getDocumento() == null ? "" : movement.getDocumento();
    }

    protected ExportedFile text(String filename, String content) {
        return new ExportedFile(filename, "text/plain",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
