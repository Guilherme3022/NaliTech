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
 * Importa um plano de contas de um arquivo Excel (.xlsx/.xls) ou CSV para um
 * cliente. As colunas sao identificadas pelo cabecalho (codigo, nome, tipo).
 */
@Service
@Transactional
public class ChartImportService {

    public record ImportResult(int contasCriadas, int contasIgnoradas) {
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

    public ImportResult importChart(UUID clienteId, String filename, byte[] content) {
        UUID empresaId = SecurityUtils.requireEmpresaId();
        clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));

        List<RawConta> contas = parse(filename, content);
        if (contas.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma conta encontrada. Verifique se o arquivo tem colunas 'codigo' e 'nome'.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        int criadas = 0;
        int ignoradas = 0;
        for (RawConta c : contas) {
            if (c.codigo == null || c.codigo.isBlank() || c.nome == null || c.nome.isBlank()) {
                ignoradas++;
                continue;
            }
            if (chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(empresaId, clienteId, c.codigo.trim())) {
                ignoradas++;
                continue;
            }
            ChartOfAccount conta = new ChartOfAccount();
            conta.setEmpresaId(empresaId);
            conta.setClienteId(clienteId);
            conta.setCodigo(c.codigo.trim());
            conta.setNome(c.nome.trim());
            conta.setTipo(c.tipo == null || c.tipo.isBlank() ? null : c.tipo.trim());
            chartRepository.save(conta);
            criadas++;
        }
        return new ImportResult(criadas, ignoradas);
    }

    private List<RawConta> parse(String filename, byte[] content) {
        String ext = extensionOf(filename);
        return switch (ext) {
            case "csv", "txt" -> parseCsv(content);
            case "xlsx", "xls" -> parseExcel(content);
            default -> throw new BusinessException(
                    "Formato nao suportado para plano de contas: " + ext + " (use CSV ou Excel).",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        };
    }

    private List<RawConta> parseCsv(byte[] content) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true)
                .setDelimiter(detectDelimiter(content)).build();
        List<RawConta> contas = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                contas.add(new RawConta(
                        get(record, CODIGO), get(record, NOME), get(record, TIPO)));
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
                contas.add(new RawConta(
                        cell(row, colCodigo, fmt), cell(row, colNome, fmt), cell(row, colTipo, fmt)));
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

    private record RawConta(String codigo, String nome, String tipo) {
    }
}
