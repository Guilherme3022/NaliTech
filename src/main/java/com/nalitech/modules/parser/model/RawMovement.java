package com.nalitech.modules.parser.model;

public record RawMovement(
        String data,
        String valor,
        String descricao,
        String documento
) {
}
