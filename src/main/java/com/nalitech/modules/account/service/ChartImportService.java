package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.ChartAccountKind;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.account.service.ChartLayoutParser.ParsedAccount;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importa um plano de contas para um cliente a partir de Excel (.xlsx/.xls) ou de TXT/CSV.
 * A leitura de texto (deteccao de layout: SPED I050, legado D-/C-, largura fixa com S/A,
 * ou delimitado com/sem cabecalho) fica em {@link ChartLayoutParser}; aqui tratamos Excel,
 * a previa e a persistencia.
 *
 * <p>Cada conta carrega a natureza {@link ChartAccountKind} (SINTETICA/ANALITICA) — usada
 * pela conciliacao e pelas sugestoes para so oferecer contas <b>analiticas</b> (lancaveis).</p>
 *
 * <p>Fluxo em dois passos: {@link #preview} devolve as contas lidas (sem persistir) e
 * {@link #confirmImport} grava apenas as escolhidas.</p>
 */
@Service
@Transactional
public class ChartImportService {

    public record ImportResult(int contasCriadas, int contasIgnoradas) {
    }

    /** Item da previa de importacao (nada foi persistido ainda). */
    public record PreviewConta(
            // Identificador unico (codigo reduzido quando houver; senao = classificacao).
            String codigo,
            // Codigo de classificacao (mascara hierarquica) — pode repetir entre contas.
            String codigoClassificacao,
            // Codigo original completo, sem remover zeros a esquerda.
            String codigoOriginal,
            String nome,
            String tipo,
            String natureza,
            Boolean analitica,
            // Natureza de saldo (DEVEDORA/CREDORA) = o que o D-/C- legado indicava. Pode ser null.
            String naturezaSaldo,
            boolean portador,
            boolean jaExiste,
            boolean importavel) {
    }

    /** Conta escolhida pelo usuario para persistir. {@code analitica} pode vir nulo (recalculado). */
    public record ContaSelecionada(
            String codigo, String codigoClassificacao, String codigoOriginal,
            String nome, String tipo, Boolean analitica, String naturezaSaldo) {
    }

    private final ChartOfAccountRepository chartRepository;
    private final ClientRepository clientRepository;
    private final ChartLayoutParser layoutParser;

    public ChartImportService(ChartOfAccountRepository chartRepository,
                              ClientRepository clientRepository,
                              ChartLayoutParser layoutParser) {
        this.chartRepository = chartRepository;
        this.clientRepository = clientRepository;
        this.layoutParser = layoutParser;
    }

    /**
     * Le o arquivo e devolve a previa das contas, sem persistir. Cada item indica a natureza
     * (S/A), se ja existe para o cliente e se e importavel (tem codigo e nome).
     */
    public List<PreviewConta> preview(UUID clienteId, String filename, byte[] content) {
        UUID empresaId = requireClient(clienteId);

        List<ParsedAccount> contas = parse(filename, content);
        if (contas.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma conta reconhecida no arquivo. Layouts aceitos: SPED ECD (registro I050), "
                            + "legado 'D-NOME,CODIGO', largura fixa 'codigo  nome  S/A' ou "
                            + "CSV/Excel com colunas 'codigo' e 'nome'.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<PreviewConta> previa = new ArrayList<>(contas.size());
        for (ParsedAccount c : contas) {
            String codigo = trimOrEmpty(c.codigo());
            String classificacao = firstNonBlank(c.codigoClassificacao(), codigo);
            String original = firstNonBlank(c.codigoOriginal(), codigo);
            String nome = trimOrEmpty(c.nome());
            boolean temCodigo = !codigo.isBlank() && !nome.isBlank();
            // Unicidade e SEMPRE pelo identificador unico (reduzido/original), nunca pela
            // classificacao — que se repete entre fornecedores do mesmo grupo.
            boolean jaExiste = temCodigo
                    && chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(empresaId, clienteId, codigo);
            Boolean analitica = c.kind().analitica();
            previa.add(new PreviewConta(
                    codigo, classificacao, original, nome, c.kind().label(), c.kind().name(),
                    analitica, c.naturezaSaldo(), c.portador(), jaExiste, temCodigo && !jaExiste));
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
            String codigo = trimOrEmpty(c.codigo());
            String nome = trimOrEmpty(c.nome());
            if (codigo.isBlank() || nome.isBlank()) {
                ignoradas++;
                continue;
            }
            // Reimportacao idempotente: a conta ja existe (mesmo identificador unico) -> ignora.
            if (chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(empresaId, clienteId, codigo)) {
                ignoradas++;
                continue;
            }
            ChartAccountKind kind = c.tipo() == null || c.tipo().isBlank()
                    ? ChartAccountKind.INDEFINIDA
                    : ChartAccountKind.normalize(c.tipo());
            ChartOfAccount conta = new ChartOfAccount();
            conta.setEmpresaId(empresaId);
            conta.setClienteId(clienteId);
            conta.setCodigo(codigo);
            conta.setCodigoClassificacao(firstNonBlank(c.codigoClassificacao(), codigo));
            conta.setCodigoOriginal(firstNonBlank(c.codigoOriginal(), codigo));
            conta.setNome(nome);
            conta.setTipo(kind.label());
            conta.setAnalitica(ChartAccountKind.resolveAnalitica(c.analitica(), c.tipo()));
            conta.setNaturezaSaldo(normalizeNaturezaSaldo(c.naturezaSaldo()));
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
                .map(p -> new ContaSelecionada(
                        p.codigo(), p.codigoClassificacao(), p.codigoOriginal(),
                        p.nome(), p.tipo(), p.analitica(), p.naturezaSaldo()))
                .toList();
        int naoImportaveis = (int) previa.stream().filter(p -> !p.importavel()).count();
        if (selecionadas.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma conta importavel: as " + naoImportaveis + " linhas lidas nao tem codigo/nome "
                            + "validos ou ja existem para este cliente. Confira o layout do arquivo.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        ImportResult result = confirmImport(clienteId, selecionadas);
        return new ImportResult(result.contasCriadas(), result.contasIgnoradas() + naoImportaveis);
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** Primeiro valor nao-vazio (apos trim); usado para dar fallback do codigo unico. */
    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    /** Normaliza a natureza de saldo para DEVEDORA/CREDORA (aceita D/C, debito/credito). */
    private static String normalizeNaturezaSaldo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().toUpperCase();
        if (t.startsWith("D")) {
            return "DEVEDORA";
        }
        if (t.startsWith("C")) {
            return "CREDORA";
        }
        return null;
    }

    private UUID requireClient(UUID clienteId) {
        UUID empresaId = SecurityUtils.requireEmpresaId();
        clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));
        return empresaId;
    }

    private List<ParsedAccount> parse(String filename, byte[] content) {
        String ext = extensionOf(filename);
        return switch (ext) {
            case "csv", "txt" -> layoutParser.parse(content);
            case "xlsx", "xls" -> ChartLayoutParser.inferHierarchy(parseExcel(content));
            default -> throw new BusinessException(
                    "Formato nao suportado para plano de contas: " + ext + " (use TXT, CSV ou Excel).",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        };
    }

    private List<ParsedAccount> parseExcel(byte[] content) {
        List<ParsedAccount> contas = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return contas;
            }
            int colCodigo = findColumn(header, fmt, ChartHeaders.CODIGO);
            int colNome = findColumn(header, fmt, ChartHeaders.NOME);
            int colTipo = findColumn(header, fmt, ChartHeaders.TIPO);
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String tipo = cell(row, colTipo, fmt);
                contas.add(ParsedAccount.simple(
                        cell(row, colCodigo, fmt), cell(row, colNome, fmt),
                        tipo, ChartAccountKind.normalize(tipo), false, null));
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

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /** Cabecalhos aceitos para o caminho de Excel. */
    private static final class ChartHeaders {
        static final List<String> CODIGO = List.of("codigo", "código", "conta", "code", "cod");
        static final List<String> NOME = List.of("nome", "descricao", "descrição", "conta_nome", "name");
        static final List<String> TIPO = List.of("tipo", "natureza", "type", "s/a", "sa");

        private ChartHeaders() {
        }
    }
}
