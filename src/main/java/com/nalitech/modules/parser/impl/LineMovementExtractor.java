package com.nalitech.modules.parser.impl;

import com.nalitech.modules.parser.model.RawMovement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai movimentacoes de texto livre (PDF/TXT) de extratos e relatorios contabeis
 * brasileiros. Como cada banco/sistema tem um layout diferente, o extrator despacha
 * para a estrategia adequada:
 *
 * <ul>
 *   <li><b>Banrisul</b> (layout com dia isolado + cabecalho de mes e debito com sinal
 *       de menos no fim): parser com estado ({@link #extractBanrisul}).</li>
 *   <li><b>Generico</b> (uma data completa {@code dd/mm/aaaa} por linha, com marcador
 *       C/D ou valor em R$): {@link #extractGeneric} — cobre Banco do Brasil,
 *       gerenciadores de caixa e relatorios de contas a pagar.</li>
 * </ul>
 *
 * Regras comuns: valor monetario exige centavos com virgula ({@code ...,dd}) para nao
 * confundir numero de documento com valor; linhas de saldo/total sao ignoradas;
 * debito vira valor negativo (essencial para casar entrada do extrato com saida do
 * sistema na conciliacao).
 */
final class LineMovementExtractor {

    // ---- Formato generico (uma data completa por linha) ----
    private static final Pattern DATE = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");
    private static final String MONEY = "\\d{1,3}(?:\\.\\d{3})*,\\d{2}";
    private static final Pattern MONEY_CD = Pattern.compile("(" + MONEY + ")\\s*([CD])\\b");
    private static final Pattern MONEY_RS = Pattern.compile("R\\$\\s*(" + MONEY + ")");
    private static final Pattern MONEY_ANY = Pattern.compile("(" + MONEY + ")");
    private static final Pattern NOISE = Pattern.compile(MONEY + "\\s*[CD]?|\\d{2}/\\d{2}/\\d{4}|R\\$");
    private static final Pattern SALDO = Pattern.compile(
            "(?i)saldo\\s+(anterior|do\\s+dia|dia|atual|final|em\\s+conta)|total\\s+(geral|do\\s+periodo)");

    // ---- Formato Banrisul (dia isolado + cabecalho de mes; debito com sinal no fim) ----
    private static final Pattern BANRISUL_MES = Pattern.compile("MOVIMENTOS\\s+([A-Z]{3})/(\\d{4})");
    private static final Pattern BANRISUL_DIA = Pattern.compile("^\\s*(\\d{2})\\s{2,}(\\S.*)$");
    private static final Pattern BANRISUL_VALOR =
            Pattern.compile("(" + MONEY + ")(-?)");
    private static final Pattern BANRISUL_NOME = Pattern.compile("(?i)^NOME:\\s*(.+)$");
    private static final Pattern BANRISUL_SALDO = Pattern.compile("(?i)saldo");
    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEV", 2), Map.entry("MAR", 3), Map.entry("ABR", 4),
            Map.entry("MAI", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AGO", 8),
            Map.entry("SET", 9), Map.entry("OUT", 10), Map.entry("NOV", 11), Map.entry("DEZ", 12));

    private LineMovementExtractor() {
    }

    static List<RawMovement> extract(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        // Layout Banrisul: identificado pelo nome do banco ou pelo cabecalho de mes.
        if (text.toUpperCase().contains("BANRISUL") || BANRISUL_MES.matcher(text).find()) {
            return extractBanrisul(text);
        }
        return extractGeneric(text);
    }

    // ---------------------------------------------------------------------------------
    // Formato generico: uma data completa por linha (BB, caixa, contas a pagar).
    // ---------------------------------------------------------------------------------
    private static List<RawMovement> extractGeneric(String text) {
        List<RawMovement> movements = new ArrayList<>();
        for (String rawLine : text.split("\\r?\\n")) {
            RawMovement movement = extractGenericLine(rawLine);
            if (movement != null) {
                movements.add(movement);
            }
        }
        return movements;
    }

    private static RawMovement extractGenericLine(String line) {
        List<String> dates = new ArrayList<>();
        Matcher dm = DATE.matcher(line);
        while (dm.find()) {
            dates.add(dm.group(1));
        }
        if (dates.isEmpty()) {
            return null;
        }

        String valor = null;
        Matcher cd = MONEY_CD.matcher(line);
        if (cd.find()) {
            valor = "D".equals(cd.group(2)) ? "-" + cd.group(1) : cd.group(1);
        } else {
            Matcher rs = MONEY_RS.matcher(line);
            if (rs.find()) {
                valor = "-" + rs.group(1);
            } else {
                Matcher any = MONEY_ANY.matcher(line);
                if (any.find()) {
                    valor = any.group(1);
                }
            }
        }
        if (valor == null) {
            return null;
        }

        String data = (line.contains("R$") && dates.size() >= 2) ? dates.get(1) : dates.get(0);
        String descricao = NOISE.matcher(line).replaceAll(" ").replaceAll("\\s+", " ").trim();
        if (SALDO.matcher(descricao).find()) {
            return null;
        }
        return new RawMovement(data, valor, descricao.isBlank() ? null : descricao, null);
    }

    // ---------------------------------------------------------------------------------
    // Formato Banrisul: dia isolado (2 digitos) + cabecalho "MOVIMENTOS MES/ANO"; o
    // debito e marcado por um sinal de menos no fim do valor; a contraparte vem na
    // linha seguinte "NOME: ...". Linhas com R$ ou "SALDO" sao resumo e ignoradas.
    // ---------------------------------------------------------------------------------
    private static List<RawMovement> extractBanrisul(String text) {
        List<RawMovement> movements = new ArrayList<>();
        int mes = 0;
        int ano = 0;
        int dia = 0;
        RawMovement[] ultimo = new RawMovement[1];

        for (String line : text.split("\\r?\\n")) {
            Matcher mh = BANRISUL_MES.matcher(line);
            if (mh.find()) {
                mes = MESES.getOrDefault(mh.group(1), 0);
                ano = Integer.parseInt(mh.group(2));
                continue;
            }
            if (mes == 0) {
                continue; // ainda no cabecalho/resumo, antes dos movimentos
            }
            // Contraparte da ultima movimentacao.
            Matcher nome = BANRISUL_NOME.matcher(line.trim());
            if (nome.find()) {
                if (ultimo[0] != null) {
                    String desc = ((ultimo[0].descricao() == null ? "" : ultimo[0].descricao())
                            + " " + nome.group(1)).replaceAll("\\s+", " ").trim();
                    RawMovement prev = ultimo[0];
                    RawMovement atualizado = new RawMovement(prev.data(), prev.valor(), desc, prev.documento());
                    movements.set(movements.size() - 1, atualizado);
                    ultimo[0] = atualizado;
                }
                continue;
            }
            if (line.contains("R$") || BANRISUL_SALDO.matcher(line).find()) {
                continue;
            }

            String conteudo = line;
            Matcher diaMatcher = BANRISUL_DIA.matcher(line);
            if (diaMatcher.find()) {
                int d = Integer.parseInt(diaMatcher.group(1));
                if (d >= 1 && d <= 31) {
                    dia = d;
                    conteudo = diaMatcher.group(2);
                }
            }
            if (dia == 0) {
                continue;
            }

            // Valor = ultimo token monetario da linha; sinal de menos no fim = debito.
            String valor = null;
            boolean negativo = false;
            Matcher vm = BANRISUL_VALOR.matcher(conteudo);
            while (vm.find()) {
                valor = vm.group(1);
                negativo = "-".equals(vm.group(2));
            }
            if (valor == null) {
                continue;
            }

            String data = String.format("%02d/%02d/%d", dia, mes, ano);
            String descricao = conteudo
                    .replaceAll("(" + MONEY + ")-?", " ")
                    .replaceAll("\\b\\d{3,}\\b", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            RawMovement movement = new RawMovement(
                    data, (negativo ? "-" : "") + valor, descricao.isBlank() ? null : descricao, null);
            movements.add(movement);
            ultimo[0] = movement;
        }
        return movements;
    }
}
