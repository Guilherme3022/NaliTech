package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.entity.MovementType;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Conciliacao;
import com.nalitech.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera o arquivo de lancamentos de uma competencia para lancar no sistema contabil.
 *
 * <p>Layout confirmado (TXT): {@code data;codigo;HISTORICO;valor;DC}, onde {@code DC} segue
 * a otica CONTABIL da conta banco: <b>D = devedora = ENTRADA</b> (dinheiro entrou),
 * <b>C = credora = SAIDA</b> (dinheiro saiu) \u2014 e o {@code valor} carrega o sinal
 * (+entrada / -saida). Parametrizavel via {@link ExportOptions} (separador decimal,
 * encoding, cabecalho, somente conciliados, saldo anterior).</p>
 *
 * <p>Pode ser gerado <b>a qualquer momento</b> (nao exige conciliacao concluida): basta
 * cliente + competencia. Movimentacoes dispensadas ({@link MovementStatus#IGNORADO}) ficam
 * de fora.</p>
 */
@Service
public class ConciliacaoExportService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int CODIGO_INICIAL = 1001;

    public record ExportFile(String filename, String contentType, byte[] content) {
    }

    /**
     * Opcoes do arquivo. Defaults batem com o layout de exemplo confirmado:
     * TXT, separador de campo ';', separador decimal '.', sem cabecalho, todas as
     * movimentacoes (exceto dispensadas), incluindo o saldo anterior.
     */
    public record ExportOptions(
            String formato,          // TXT | CSV
            char separadorDecimal,   // '.' (padrao do exemplo) ou ','
            String encoding,         // ex.: "UTF-8" ou "Windows-1252"
            boolean incluirCabecalho,
            boolean somenteConciliados,
            boolean incluirSaldoAnterior) {

        public static ExportOptions padrao() {
            return new ExportOptions("TXT", '.', "UTF-8", false, false, true);
        }
    }

    private final ConciliacaoService conciliacaoService;
    private final MovementRepository movementRepository;

    public ConciliacaoExportService(ConciliacaoService conciliacaoService,
                                    MovementRepository movementRepository) {
        this.conciliacaoService = conciliacaoService;
        this.movementRepository = movementRepository;
    }

    /** Export do arquivo oficial de uma conciliacao concluida (fechamento). */
    @Transactional(readOnly = true)
    public ExportFile export(UUID conciliacaoId, String formato) {
        Conciliacao conciliacao = conciliacaoService.requireConcluida(conciliacaoId);
        ExportOptions opts = withFormato(ExportOptions.padrao(), formato);
        return exportCompetencia(conciliacao.getEmpresaId(), conciliacao.getClienteId(),
                conciliacao.getCompetencia(), opts,
                "conciliacao-" + conciliacaoId.toString().substring(0, 8));
    }

    /**
     * Export sob demanda por cliente/competencia (nao exige conciliacao concluida):
     * o usuario gera o TXT quando quiser para lancar no sistema contabil.
     */
    @Transactional(readOnly = true)
    public ExportFile exportCompetencia(UUID empresaId, UUID clienteId, LocalDate competencia,
                                        ExportOptions opts) {
        String base = "lancamentos-" + competencia.getYear()
                + String.format("%02d", competencia.getMonthValue());
        return exportCompetencia(empresaId, clienteId, competencia, opts, base);
    }

    private ExportFile exportCompetencia(UUID empresaId, UUID clienteId, LocalDate competencia,
                                         ExportOptions opts, String nomeBase) {
        if (clienteId == null || competencia == null) {
            throw new BusinessException("Informe cliente e competencia para gerar o arquivo.",
                    HttpStatus.BAD_REQUEST);
        }
        String fmt = opts.formato() == null ? "TXT" : opts.formato().trim().toUpperCase();
        if (!fmt.equals("TXT") && !fmt.equals("CSV")) {
            throw new BusinessException("Formato nao suportado: " + fmt + " (use TXT ou CSV).",
                    HttpStatus.BAD_REQUEST);
        }

        LocalDate inicio = competencia.withDayOfMonth(1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        List<Movement> movimentos = movementRepository
                .findByEmpresaIdAndClienteIdAndDataBetweenOrderByData(empresaId, clienteId, inicio, fim);

        String sep = ";";
        StringBuilder sb = new StringBuilder();
        if (opts.incluirCabecalho()) {
            sb.append(String.join(sep, "data", "codigo", "historico", "valor", "dc")).append('\n');
        }
        int codigo = CODIGO_INICIAL;
        for (Movement m : movimentos) {
            // Dispensadas nunca entram no arquivo.
            if (m.getStatus() == MovementStatus.IGNORADO) {
                continue;
            }
            if (opts.somenteConciliados() && m.getStatus() != MovementStatus.CONCILIADO
                    && m.getStatus() != MovementStatus.CLASSIFICADO) {
                continue;
            }
            boolean saldo = ehSaldoAnterior(m.getDescricao());
            if (saldo && !opts.incluirSaldoAnterior()) {
                continue;
            }
            sb.append(String.join(sep,
                    m.getData() == null ? "" : DATA_BR.format(m.getData()),
                    String.valueOf(codigo++),
                    historico(m.getDescricao()),
                    valorComSinal(m, opts.separadorDecimal()),
                    naturezaDC(m)))
              .append('\n');
        }

        String ext = fmt.equals("CSV") ? "csv" : "txt";
        String contentType = fmt.equals("CSV") ? "text/csv" : "text/plain";
        Charset charset = resolveCharset(opts.encoding());
        String filename = nomeBase + "." + ext;
        return new ExportFile(filename, contentType, sb.toString().getBytes(charset));
    }

    private ExportOptions withFormato(ExportOptions opts, String formato) {
        if (formato == null || formato.isBlank()) {
            return opts;
        }
        return new ExportOptions(formato, opts.separadorDecimal(), opts.encoding(),
                opts.incluirCabecalho(), opts.somenteConciliados(), opts.incluirSaldoAnterior());
    }

    /** D = ENTRADA (devedora, dinheiro entrou no banco); C = SAIDA (credora, saiu). */
    private String naturezaDC(Movement m) {
        return m.getTipo() == MovementType.SAIDA ? "C" : "D";
    }

    /** Valor com sinal (+entrada / -saida), 2 casas, separador decimal configuravel. */
    private String valorComSinal(Movement m, char separadorDecimal) {
        BigDecimal v = m.getValor() == null ? BigDecimal.ZERO : m.getValor();
        // Normaliza o sinal a partir do tipo (o valor guardado ja costuma vir com sinal,
        // mas garantimos coerencia: SAIDA sempre negativa, ENTRADA sempre positiva).
        BigDecimal abs = v.abs().setScale(2, RoundingMode.HALF_UP);
        BigDecimal comSinal = m.getTipo() == MovementType.SAIDA ? abs.negate() : abs;
        String s = comSinal.toPlainString();
        return separadorDecimal == ',' ? s.replace('.', ',') : s;
    }

    private String historico(String descricao) {
        if (descricao == null) {
            return "";
        }
        return descricao.replace(';', ' ').replace('\n', ' ').replace('\r', ' ')
                .replaceAll("\\s+", " ").trim().toUpperCase();
    }

    private boolean ehSaldoAnterior(String descricao) {
        if (descricao == null) {
            return false;
        }
        String d = descricao.toLowerCase();
        return d.contains("saldo anterior") || d.contains("saldo ant");
    }

    private Charset resolveCharset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return java.nio.charset.StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding.trim());
        } catch (RuntimeException ex) {
            return java.nio.charset.StandardCharsets.UTF_8;
        }
    }
}
