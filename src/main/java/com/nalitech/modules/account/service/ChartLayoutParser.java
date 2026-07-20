package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.ChartAccountKind;
import com.nalitech.shared.exception.BusinessException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Le um plano de contas em TEXTO (TXT/CSV) detectando automaticamente o layout. Suporta:
 * <ol>
 *   <li><b>SPED ECD</b> — registro {@code |I050|...} (padrao da Escrituracao Contabil Digital);</li>
 *   <li><b>Legado</b> — linhas {@code D-NOME,CODIGO} / {@code C-PORTADOR|NOME,CODIGO};</li>
 *   <li><b>Largura fixa</b> — {@code <seq><codigo>   NOME   <S|A>} (colunas posicionais);</li>
 *   <li><b>Delimitado</b> — {@code ,}/{@code ;}/{@code |}/tab, com ou sem cabecalho.</li>
 * </ol>
 *
 * <p>Quando o arquivo nao informa o tipo (S/A) da conta, a natureza e inferida pela
 * hierarquia do codigo: um codigo que e prefixo de outro e SINTETICA; as folhas sao
 * ANALITICAS.</p>
 *
 * <p>Nao persiste nada — devolve apenas {@link ParsedAccount}s para o servico de importacao.</p>
 */
@Component
public class ChartLayoutParser {

    /** Conta lida do arquivo (ainda nao persistida). */
    public record ParsedAccount(
            String codigo, String nome, String tipoRaw, ChartAccountKind kind, boolean portador,
            // Natureza de saldo da conta: "DEVEDORA" / "CREDORA" / null. E o que o layout
            // legado D-/C- representava (D = devedora, C = credora). Nao confundir com o
            // lado do lancamento (partida dobrada), que e por movimentacao.
            String naturezaSaldo) {
    }

    private static final List<String> CODIGO_HEADERS =
            List.of("codigo", "conta", "code", "cod", "cod_cta", "classificacao", "reduzida");
    private static final List<String> NOME_HEADERS =
            List.of("nome", "descricao", "conta_nome", "name", "cta", "titulo", "historico");
    private static final List<String> TIPO_HEADERS =
            List.of("tipo", "natureza", "type", "ind_cta", "sa", "s_a", "grau", "nivel");

    private static final Pattern FIXED_LINE = Pattern.compile("^(\\d+)(\\s+)(\\S.*)$");

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    /**
     * Decodifica os bytes do arquivo para texto. Tenta UTF-8 de forma estrita; se o arquivo
     * nao for UTF-8 valido (ex.: Windows-1252 / Latin-1, comum em exportacoes de sistemas
     * contabeis brasileiros), cai para Windows-1252 — evitando o caractere de substituicao
     * (ex.: "DISPON\uFFFDVEL" no lugar de "DISPONIVEL").
     */
    public static String decode(byte[] content) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException ex) {
            return new String(content, WINDOWS_1252);
        }
    }

    /** Le o conteudo texto e devolve as contas, ja com a natureza (kind) resolvida. */
    public List<ParsedAccount> parse(byte[] content) {
        String text = decode(content);
        List<String> rawLines = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (!line.isBlank()) {
                // Remove espacos/tabs/CR finais: no layout de largura fixa a flag S/A fica na
                // ultima coluna util, seguida de preenchimento em branco ate a largura do registro.
                rawLines.add(line.stripTrailing());
            }
        }
        if (rawLines.isEmpty()) {
            return List.of();
        }

        List<ParsedAccount> contas;
        if (isSped(rawLines)) {
            contas = parseSped(rawLines);
        } else if (isLegacy(rawLines)) {
            contas = parseLegacy(rawLines);
        } else if (isFixedWidth(rawLines)) {
            contas = parseFixedWidth(rawLines);
        } else {
            contas = parseDelimited(text);
        }
        return inferHierarchy(contas);
    }

    /**
     * Completa a natureza (S/A) das contas ainda INDEFINIDAS usando a hierarquia do codigo:
     * um codigo que e prefixo de outro codigo distinto e SINTETICO; caso contrario, ANALITICO.
     * Publico para o caminho de Excel reaproveitar a mesma regra.
     */
    public static List<ParsedAccount> inferHierarchy(List<ParsedAccount> contas) {
        List<String> codigos = contas.stream()
                .map(ParsedAccount::codigo)
                .filter(c -> c != null && !c.isBlank())
                .toList();
        List<ParsedAccount> result = new ArrayList<>(contas.size());
        for (ParsedAccount c : contas) {
            if (c.kind() != ChartAccountKind.INDEFINIDA || c.codigo() == null || c.codigo().isBlank()) {
                result.add(c);
                continue;
            }
            boolean temFilho = codigos.stream()
                    .anyMatch(outro -> !outro.equals(c.codigo()) && outro.startsWith(c.codigo()));
            ChartAccountKind kind = temFilho ? ChartAccountKind.SINTETICA : ChartAccountKind.ANALITICA;
            result.add(new ParsedAccount(
                    c.codigo(), c.nome(), c.tipoRaw(), kind, c.portador(), c.naturezaSaldo()));
        }
        return result;
    }

    // ---------------------------------------------------------------- SPED ECD (|I050|)

    private boolean isSped(List<String> lines) {
        return lines.stream().anyMatch(l -> l.contains("|I050|"));
    }

    /** Layout I050: {@code |I050|DT_ALT|COD_NAT|IND_CTA|NIVEL|COD_CTA|COD_CTA_SUP|CTA|}. */
    private List<ParsedAccount> parseSped(List<String> lines) {
        List<ParsedAccount> contas = new ArrayList<>();
        for (String line : lines) {
            if (!line.contains("|I050|")) {
                continue;
            }
            String[] f = line.split("\\|", -1);
            // f[0] vazio, f[1]=I050, f[2]=DT_ALT, f[3]=COD_NAT, f[4]=IND_CTA, f[5]=NIVEL,
            // f[6]=COD_CTA, f[7]=COD_CTA_SUP, f[8]=CTA
            if (f.length < 9) {
                continue;
            }
            String indCta = f[4].trim();
            String codigo = f[6].trim();
            String nome = f[8].trim();
            contas.add(new ParsedAccount(
                    codigo, nome, indCta, ChartAccountKind.normalize(indCta), false, null));
        }
        return contas;
    }

    // ---------------------------------------------------------------- Legado (D-/C-)

    private boolean isLegacy(List<String> lines) {
        String first = lines.get(0).trim().toUpperCase();
        return first.startsWith("D-") || first.startsWith("C-");
    }

    private List<ParsedAccount> parseLegacy(List<String> lines) {
        List<ParsedAccount> contas = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            String prefix = line.substring(0, Math.min(2, line.length())).toUpperCase();
            if (!prefix.startsWith("D-") && !prefix.startsWith("C-")) {
                continue;
            }
            String rest = line.substring(2);
            int lastComma = rest.lastIndexOf(',');
            String codigo = lastComma >= 0 ? rest.substring(lastComma + 1).trim() : "";
            String nomeParte = lastComma >= 0 ? rest.substring(0, lastComma).trim() : rest.trim();

            boolean portador = false;
            String nome = nomeParte;
            int pipe = nomeParte.indexOf('|');
            if (pipe >= 0) {
                portador = nomeParte.substring(0, pipe).trim().equalsIgnoreCase("PORTADOR");
                nome = nomeParte.substring(pipe + 1).trim();
            }
            // O layout legado nao distingue S/A (a hierarquia resolve); mas o D-/C- indica
            // a natureza de saldo: D = devedora, C = credora.
            String naturezaSaldo = prefix.startsWith("D-") ? "DEVEDORA" : "CREDORA";
            contas.add(new ParsedAccount(
                    codigo, nome, null, ChartAccountKind.INDEFINIDA, portador, naturezaSaldo));
        }
        return contas;
    }

    // ---------------------------------------------------------------- Largura fixa (S/A)

    /** Reconhece o layout posicional: maioria das linhas comeca por digitos + gap e tem S/A ao final. */
    private boolean isFixedWidth(List<String> lines) {
        int matches = 0;
        int flagged = 0;
        int recordWidth = modeLength(lines);
        for (String line : lines) {
            if (!FIXED_LINE.matcher(line).matches()) {
                continue;
            }
            matches++;
            int flagCol = recordWidth - 1;
            if (flagCol >= 0 && flagCol < line.length()) {
                ChartAccountKind k = ChartAccountKind.normalize(String.valueOf(line.charAt(flagCol)));
                if (k != ChartAccountKind.INDEFINIDA) {
                    flagged++;
                }
            }
        }
        return matches >= Math.max(1, lines.size() * 6 / 10) && flagged >= matches * 6 / 10;
    }

    private List<ParsedAccount> parseFixedWidth(List<String> lines) {
        List<String> digitBlocks = new ArrayList<>();
        List<Matcher> matched = new ArrayList<>();
        List<String> matchedLines = new ArrayList<>();
        for (String line : lines) {
            Matcher m = FIXED_LINE.matcher(line);
            if (m.matches()) {
                digitBlocks.add(m.group(1));
                matched.add(m);
                matchedLines.add(line);
            }
        }
        int counterWidth = detectCounterWidth(digitBlocks);
        int nameStart = modeNameStart(matched);
        int flagCol = modeLength(matchedLines) - 1;

        List<ParsedAccount> contas = new ArrayList<>();
        for (int i = 0; i < matchedLines.size(); i++) {
            String line = matchedLines.get(i);
            String digits = digitBlocks.get(i);
            String codigo = counterWidth > 0 && digits.length() > counterWidth
                    ? digits.substring(counterWidth)
                    : digits;

            String tipoRaw = null;
            ChartAccountKind kind = ChartAccountKind.INDEFINIDA;
            if (flagCol >= 0 && flagCol < line.length()) {
                tipoRaw = String.valueOf(line.charAt(flagCol));
                kind = ChartAccountKind.normalize(tipoRaw);
            }

            int nameEnd = Math.min(flagCol < 0 ? line.length() : flagCol, line.length());
            int start = Math.min(nameStart, nameEnd);
            String nome = line.substring(start, nameEnd).trim();

            contas.add(new ParsedAccount(codigo.trim(), nome, tipoRaw, kind, false, null));
        }
        return contas;
    }

    /** Descobre a largura de um contador sequencial no inicio do codigo (ex.: 0000001, 0000002...). */
    private int detectCounterWidth(List<String> digitBlocks) {
        for (int w = 4; w <= 9; w++) {
            int ok = 0;
            int total = 0;
            Long prev = null;
            for (String d : digitBlocks) {
                if (d.length() <= w) {
                    prev = null;
                    continue;
                }
                long value;
                try {
                    value = Long.parseLong(d.substring(0, w));
                } catch (NumberFormatException ex) {
                    prev = null;
                    continue;
                }
                if (prev != null) {
                    total++;
                    if (value == prev + 1) {
                        ok++;
                    }
                }
                prev = value;
            }
            if (total > 0 && ok >= total * 0.8) {
                return w;
            }
        }
        return 0;
    }

    private int modeNameStart(List<Matcher> matched) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (Matcher m : matched) {
            freq.merge(m.end(2), 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private int modeLength(List<String> lines) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (String line : lines) {
            freq.merge(line.length(), 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    // ---------------------------------------------------------------- Delimitado (com/sem cabecalho)

    private List<ParsedAccount> parseDelimited(String text) {
        char delimiter = detectDelimiter(text);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true).setTrim(true).setDelimiter(delimiter).build();
        List<CSVRecord> records = new ArrayList<>();
        try (StringReader reader = new StringReader(text);
             CSVParser parser = format.parse(reader)) {
            records.addAll(parser.getRecords());
        } catch (Exception ex) {
            throw new BusinessException("Falha ao ler arquivo delimitado: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (records.isEmpty()) {
            return List.of();
        }
        return looksLikeHeader(records.get(0))
                ? parseWithHeader(records)
                : parsePositional(records);
    }

    private boolean looksLikeHeader(CSVRecord first) {
        for (int i = 0; i < first.size(); i++) {
            String value = norm(first.get(i));
            if (CODIGO_HEADERS.contains(value) || NOME_HEADERS.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private List<ParsedAccount> parseWithHeader(List<CSVRecord> records) {
        CSVRecord header = records.get(0);
        int colCodigo = indexOf(header, CODIGO_HEADERS);
        int colNome = indexOf(header, NOME_HEADERS);
        int colTipo = indexOf(header, TIPO_HEADERS);
        List<ParsedAccount> contas = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            CSVRecord row = records.get(i);
            String tipo = at(row, colTipo);
            contas.add(new ParsedAccount(
                    at(row, colCodigo), at(row, colNome), tipo,
                    ChartAccountKind.normalize(tipo), false, null));
        }
        return contas;
    }

    /** Sem cabecalho: coluna 0 = codigo, coluna 1 = nome, coluna 2 (se houver) = tipo. */
    private List<ParsedAccount> parsePositional(List<CSVRecord> records) {
        List<ParsedAccount> contas = new ArrayList<>();
        for (CSVRecord row : records) {
            if (row.size() < 2) {
                continue;
            }
            String tipo = row.size() >= 3 ? row.get(2) : null;
            contas.add(new ParsedAccount(
                    row.get(0), row.get(1), tipo, ChartAccountKind.normalize(tipo), false, null));
        }
        return contas;
    }

    private int indexOf(CSVRecord header, List<String> candidates) {
        for (int i = 0; i < header.size(); i++) {
            if (candidates.contains(norm(header.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private String at(CSVRecord row, int col) {
        return col >= 0 && col < row.size() ? row.get(col) : null;
    }

    private String norm(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase();
    }

    private char detectDelimiter(String text) {
        String head = text.substring(0, Math.min(text.length(), 1024));
        long pipes = head.chars().filter(c -> c == '|').count();
        long tabs = head.chars().filter(c -> c == '\t').count();
        long semicolons = head.chars().filter(c -> c == ';').count();
        long commas = head.chars().filter(c -> c == ',').count();
        long max = Math.max(Math.max(pipes, tabs), Math.max(semicolons, commas));
        if (max == 0) {
            return ',';
        }
        if (max == pipes) {
            return '|';
        }
        if (max == tabs) {
            return '\t';
        }
        return max == semicolons ? ';' : ',';
    }

}
