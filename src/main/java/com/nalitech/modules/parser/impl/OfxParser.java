package com.nalitech.modules.parser.impl;

import com.nalitech.modules.parser.DocumentParser;
import com.nalitech.modules.parser.model.ParseResult;
import com.nalitech.modules.parser.model.RawMovement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OfxParser implements DocumentParser {

    private static final Pattern TRANSACTION = Pattern.compile(
            "<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String extension) {
        return "ofx".equals(extension);
    }

    @Override
    public ParseResult parse(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        List<RawMovement> movements = new ArrayList<>();
        Matcher matcher = TRANSACTION.matcher(text);
        while (matcher.find()) {
            String block = matcher.group(1);
            String descricao = firstNonNull(tag(block, "MEMO"), tag(block, "NAME"));
            movements.add(new RawMovement(
                    tag(block, "DTPOSTED"),
                    tag(block, "TRNAMT"),
                    descricao,
                    tag(block, "FITID"),
                    tipoIndicador(tag(block, "TRNTYPE"))));
        }
        return ParseResult.of(movements);
    }

    private String tag(String block, String tag) {

        Matcher matcher = Pattern.compile(
                "<" + tag + ">([^<\\r\\n]*)", Pattern.CASE_INSENSITIVE).matcher(block);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    /**
     * Traduz o TRNTYPE do OFX para a convencao do extrato (D = saida, C = entrada).
     * O TRNAMT ja vem com sinal no OFX, mas o TRNTYPE torna a deteccao robusta quando
     * algum banco envia o valor sem sinal. Tipos ambiguos retornam null (usa-se o sinal).
     */
    private String tipoIndicador(String trnType) {
        if (trnType == null) {
            return null;
        }
        String t = trnType.trim().toUpperCase();
        if (t.startsWith("CREDIT") || t.equals("DEP") || t.equals("DIRECTDEP")
                || t.equals("INT") || t.equals("DIV")) {
            return "C";
        }
        if (t.startsWith("DEBIT") || t.equals("PAYMENT") || t.equals("FEE") || t.equals("SRVCHG")
                || t.equals("ATM") || t.equals("POS") || t.equals("CHECK") || t.equals("CASH")
                || t.equals("DIRECTDEBIT") || t.equals("REPEATPMT")) {
            return "D";
        }
        return null;
    }
}
