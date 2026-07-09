package com.ledgerflow.modules.parser.impl;

import java.util.List;
import java.util.Map;

final class ColumnResolver {

    static final List<String> DATA = List.of("data", "date", "dt", "data lancamento", "data mov");
    static final List<String> VALOR = List.of("valor", "value", "amount", "montante", "vlr");
    static final List<String> DESCRICAO =
            List.of("descricao", "historico", "description", "memo", "lancamento", "detalhe");
    static final List<String> DOCUMENTO = List.of("documento", "doc", "numero", "num", "fitid");

    private ColumnResolver() {
    }

    static int indexOf(List<String> headers, List<String> candidates) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i) == null ? "" : headers.get(i).trim().toLowerCase();
            if (candidates.contains(header)) {
                return i;
            }
        }
        return -1;
    }

    static String at(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }

    static String fromMap(Map<String, String> row, List<String> candidates) {
        for (var entry : row.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
            if (candidates.contains(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
