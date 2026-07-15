package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importa um plano de contas para um cliente a partir de:
 * <ul>
 *   <li>Excel (.xlsx/.xls) ou CSV <b>com cabecalho</b> (colunas codigo, nome, tipo);</li>
 *   <li>TXT no layout do sistema legado: {@code D-NOME,CODIGO} / {@code C-NOME,CODIGO},
 *       onde {@code D-} = natureza Debito e {@code C-} = natureza Credito. Um marcador
 *       {@code PORTADOR|} antes do nome (ex.: {@code C-PORTADOR|BANCO BANRISUL,527})
 *       identifica contas de portador. Linhas sem codigo sao mantidas na previa, mas
 *       marcadas como nao importaveis.</li>
 * </ul>
 *
 * <p>A importacao e feita em dois passos: {@link #preview} devolve as contas lidas (sem
 * persistir) para o usuario revisar/selecionar, e {@link #confirmImport} persiste apenas
 * as contas escolhidas.</p>
 */
@Service
@Transactional
public class ChartImportService {

    public record ImportResult(int contasCriadas, int contasIgnoradas) {
    }

    /** Item da previa de importacao (nada foi persistido ainda). */
    public record PreviewConta(
            String codigo,
            String nome,
            String tipo,
            String natureza,
            boolean portador,
            boolean jaExiste,
            boolean importavel) {
    }

    /** Conta escolhida pelo usuario para persistir. */
    public record ContaSelecionada(String codigo, String nome, String tipo) {
    }

    private static final List<String> CODIGO = List.of("codigo", "código", "conta", "code", "cod");
    private static final List<String> NOME = List.of("nome", "descricao", "descrição", "conta_nome", "name");
    private static final List<String> TIPO = List.of("tipo", "natureza", "type");

    private final ChartOfAccountRepository chartRepository;
    private final ClientRepository clientRepository;

    public ChartImportService(ChartOfAccountRepository chartRepository,
                              ClientRepository clientRepository) {
        this.chartRepository = chartRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Le o arquivo e devolve a previa das contas, sem persistir. Cada item indica se ja
     * existe para o cliente e se e importavel (tem codigo).
     */
    public List<PreviewConta> preview(UUID clienteId, String filename, byte[] content) {
        UUID empresaId = requireClient(clienteId);

        List<RawConta> contas = parse(filename, content);
        if (contas.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma conta encontrada. Use o layout 'D-NOME,CODIGO' (TXT) ou um "
                            + "arquivo com colunas 'codigo' e 'nome'.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<PreviewConta> previa = new ArrayList<>(contas.size());
        for (RawConta c : contas) {
            String codigo = c.codigo == null ? "" : c.codigo.trim();
            String nome = c.nome == null ? "" : c.nome.trim();
            boolean temCodigo = !codigo.isBlank() && !nome.isBlank();
            boolean jaExiste = temCodigo
                    && chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(empresaId, clienteId, codigo);
            previa.add(new PreviewConta(
                    codigo, nome, c.tipo, c.natureza, c.portador, jaExiste, temCodigo && !jaExiste));
        }
        return previa;
    }

    /** Persiste apenas as contas selecionadas pelo usuario. */
    public ImportResult confirmImport(UUID clienteId, List<ContaSelecionada> contas) {
        UUID empresaId = requireClient(clienteId);
        if (contas == null || contas.isEmpty()) {
            throw new BusinessException("Nenhuma conta selecionada para importar.",
                    HttpStatus.BAD_REQUEST);
        }

        int criadas = 0;
        int ignoradas = 0;
        for (ContaSelecionada c : contas) {
            String codigo = c.codigo() == null ? "" : c.codigo().trim();
            String nome = c.nome() == null ? "" : c.nome().trim();
            if (codigo.isBlank() || nome.isBlank()) {
                ignoradas++;
                continue;
            }
            if (chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(empresaId, clienteId, codigo)) {
                ignoradas++;
                continue;
            }
            ChartOfAccount conta = new ChartOfAccount();
            conta.setEmpresaId(empresaId);
            conta.setClienteId(clienteId);
            conta.setCodigo(codigo);
            conta.setNome(nome);
            conta.setTipo(c.tipo() == null || c.tipo().isBlank() ? null : c.tipo().trim());
            chartRepository.save(conta);
            criadas++;
        }
        return new ImportResult(criadas, ignoradas);
    }

    /**
     * Importacao direta (sem previa): le e persiste tudo de uma vez. Mantida para
     * compatibilidade com o fluxo antigo.
     */
    public ImportResult importChart(UUID clienteId, String filename, byte[] content) {
        List<PreviewConta> previa = preview(clienteId, filename, content);
        List<ContaSelecionada> selecionadas = previa.stream()
                .filter(PreviewConta::importavel)
                .map(p -> new ContaSelecionada(p.codigo(), p.nome(), p.tipo()))
                .toList();
        int naoImportaveis = (int) previa.stream().filter(p -> !p.importavel()).count();
        ImportResult result = confirmImport(clienteId, selecionadas);
        return new ImportResult(result.contasCriadas(), result.contasIgnoradas() + naoImportaveis);
    }

    private UUID requireClient(UUID clienteId) {
        UUID empresaId = SecurityUtils.requireEmpresaId();
        clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));
        return empresaId;
    }

    private List<RawConta> parse(String filename, byte[] content) {
        String ext = extensionOf(filename);
        return switch (ext) {
            case "csv", "txt" -> parseText(content);
            case "xlsx", "xls" -> parseExcel(content);
            default -> throw new BusinessException(
                    "Formato nao suportado para plano de contas: " + ext + " (use TXT, CSV ou Excel).",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        };
    }

    /** Detecta o layout legado (linhas iniciando com D-/C-) ou cai no CSV com cabecalho. */
    private List<RawConta> parseText(byte[] content) {
        return isLegacyLayout(content) ? parseLegacy(content) : parseCsv(content);
    }

    private boolean isLegacyLayout(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String upper = trimmed.toUpperCase();
            return upper.startsWith("D-") || upper.startsWith("C-");
        }
        return false;
    }

    /**
     * Le o layout {@code D-NOME,CODIGO} / {@code C-PORTADOR|NOME,CODIGO}. O codigo e o que
     * vem apos a ultima virgula (pode estar vazio); o resto e nome + eventual marcador.
     */
    private List<RawConta> parseLegacy(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        List<RawConta> contas = new ArrayList<>();
        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String upper = line.substring(0, Math.min(2, line.length())).toUpperCase();
            String natureza;
            if (upper.startsWith("D-")) {
                natureza = "DEBITO";
            } else if (upper.startsWith("C-")) {
                natureza = "CREDITO";
            } else {
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
                String marcador = nomeParte.substring(0, pipe).trim();
                portador = marcador.equalsIgnoreCase("PORTADOR");
                nome = nomeParte.substring(pipe + 1).trim();
            }
            contas.add(new RawConta(codigo, nome, natureza, natureza, portador));
        }
        return contas;
    }

    private List<RawConta> parseCsv(byte[] content) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true)
                .setDelimiter(detectDelimiter(content)).build();
        List<RawConta> contas = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                String tipo = get(record, TIPO);
                contas.add(new RawConta(get(record, CODIGO), get(record, NOME), tipo, tipo, false));
            }
        } catch (Exception ex) {
            throw new BusinessException("Falha ao ler CSV: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return contas;
    }

    private List<RawConta> parseExcel(byte[] content) {
        List<RawConta> contas = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return contas;
            }
            int colCodigo = findColumn(header, fmt, CODIGO);
            int colNome = findColumn(header, fmt, NOME);
            int colTipo = findColumn(header, fmt, TIPO);
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String tipo = cell(row, colTipo, fmt);
                contas.add(new RawConta(
                        cell(row, colCodigo, fmt), cell(row, colNome, fmt), tipo, tipo, false));
            }
        } catch (Exception ex) {
            throw new BusinessException("Falha ao ler Excel: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return contas;
    }

    private int findColumn(Row header, DataFormatter fmt, List<String> candidates) {
        for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
            String value = cell(header, c, fmt);
            if (value != null && candidates.contains(value.trim().toLowerCase())) {
                return c;
            }
        }
        return -1;
    }

    private String cell(Row row, int col, DataFormatter fmt) {
        if (col < 0 || row.getCell(col) == null) {
            return null;
        }
        return fmt.formatCellValue(row.getCell(col));
    }

    private String get(CSVRecord record, List<String> candidates) {
        for (String header : record.getParser().getHeaderNames()) {
            if (header != null && candidates.contains(header.trim().toLowerCase())) {
                return record.get(header);
            }
        }
        return null;
    }

    private char detectDelimiter(byte[] content) {
        String head = new String(content, 0, Math.min(content.length, 512), StandardCharsets.UTF_8);
        long semicolons = head.chars().filter(c -> c == ';').count();
        long commas = head.chars().filter(c -> c == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private record RawConta(String codigo, String nome, String tipo, String natureza, boolean portador) {
    }
}
