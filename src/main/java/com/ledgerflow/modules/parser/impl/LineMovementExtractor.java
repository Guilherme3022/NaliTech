package com.ledgerflow.modules.parser.impl;

import com.ledgerflow.modules.parser.model.RawMovement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LineMovementExtractor {

    private static final Pattern DATE = Pattern.compile("\\b(\\d{2}[/.-]\\d{2}[/.-]\\d{2,4})\\b");
    private static final Pattern AMOUNT = Pattern.compile("(-?\\d{1,3}(?:[.\\s]?\\d{3})*[,.]\\d{2})");

    private LineMovementExtractor() {
    }

    static List<RawMovement> extract(String text) {
        List<RawMovement> movements = new ArrayList<>();
        if (text == null) {
            return movements;
        }
        for (String line : text.split("\\r?\\n")) {
            Matcher dateMatcher = DATE.matcher(line);
            Matcher amountMatcher = AMOUNT.matcher(line);
            if (dateMatcher.find() && amountMatcher.find()) {
                String data = dateMatcher.group(1);
                String valor = amountMatcher.group(1);
                String descricao = line
                        .replace(data, "")
                        .replace(valor, "")
                        .replaceAll("\\s+", " ")
                        .trim();
                movements.add(new RawMovement(data, valor, descricao, null));
            }
        }
        return movements;
    }
}
