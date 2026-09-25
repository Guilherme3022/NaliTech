package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.client.entity.Client;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Conciliacao;
import com.nalitech.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera o arquivo de lancamentos (partida dobrada) de uma competencia para lancar no
 * sistema contabil.
 *
 * <p><b>Layout TXT confirmado</b> (campos separados por {@code ;}):
 * {@code data;contaDebito;;contaCredito;;valor;historico;codigoEmpresaContabil}</p>
 *
 * <pre>10/07/2026;4921;;1362;;477,20;VLR REF PAGTO BCONTROL 1191 PARC 1;47</pre>
 *
 * <ul>
 *   <li>contas = {@code ChartOfAccount.codigo} de {@code contaDebitoId}/{@code contaCreditoId}
 *       (vazio quando a conta ainda nao foi escolhida);</li>
 *   <li>valor = positivo, 2 casas, separador <b>virgula</b>, sem sinal (a direcao vem das contas);</li>
 *   <li>historico = descricao do movimento em MAIUSCULAS;</li>
 *   <li>ultimo campo = {@code codigoEmpresaContabil} do cliente;</li>
 *   <li>campos 3 e 5 = vazios (reservados).</li>
 * </ul>
 *
 * <p>Pode ser gerado a qualquer momento (nao exige conciliacao concluida). Movimentacoes
 * dispensadas ({@link MovementStatus#IGNORADO}) ficam de fora.</p>
 */
@Service
public class ConciliacaoExportService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record ExportFile(String filename, String contentType, byte[] content) {
    }

    /**
     * Opcoes do arquivo. Defaults batem com o layout confirmado: TXT, vírgula decimal,
     * UTF-8, sem cabecalho, todas as movimentacoes (exceto dispensadas), incluindo saldo.
     */
    public record ExportOptions(
            String formato,          // TXT | CSV
            char separadorDecimal,   // ',' (padrao) ou '.'
            String encoding,
            boolean incluirCabecalho,
            boolean somenteConciliados,
            boolean incluirSaldoAnterior) {

        public static ExportOptions padrao() {
            return new ExportOptions("TXT", ',', "UTF-8", false, false, true);
        }
    }

    private final ConciliacaoService conciliacaoService;
    private final MovementRepository movementRepository;
    private final ChartOfAccountRepository chartRepository;
    private final ClientRepository clientRepository;

    public ConciliacaoExportService(ConciliacaoService conciliacaoService,
                                    MovementRepository movementRepository,
                                    ChartOfAccountRepository chartRepository,
                                    ClientRepository clientRepository) {
        this.conciliacaoService = conciliacaoService;
        this.movementRepository = movementRepository;
        this.chartRepository = chartRepository;
        this.clientRepository = clientRepository;
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

    /** Export sob demanda por cliente/competencia (nao exige conciliacao concluida). */
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

        // Resolve codigos das contas (id -> codigo) em lote.
        Set<UUID> contaIds = new HashSet<>();
        for (Movement m : movimentos) {
            if (m.getContaDebitoId() != null) {
                contaIds.add(m.getContaDebitoId());
            }
            if (m.getContaCreditoId() != null) {
                contaIds.add(m.getContaCreditoId());
            }
        }
        Map<UUID, String> codigoPorConta = contaIds.isEmpty() ? Map.of()
                : chartRepository.findAllById(contaIds).stream()
                        .filter(c -> c.getCodigo() != null)
                        .collect(Collectors.toMap(ChartOfAccount::getId, ChartOfAccount::getCodigo));

        String codigoEmpresa = clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .map(Client::getCodigoEmpresaContabil)
                .map(String::valueOf)
                .orElse("");

        String sep = ";";
        StringBuilder sb = new StringBuilder();
        if (opts.incluirCabecalho()) {
            sb.append(String.join(sep, "data", "conta_debito", "", "conta_credito", "",
                    "valor", "historico", "empresa")).append('\n');
        }
        for (Movement m : movimentos) {
            if (m.getStatus() == MovementStatus.IGNORADO) {
                continue; // dispensadas nunca entram
            }
            if (opts.somenteConciliados() && m.getStatus() != MovementStatus.CONCILIADO
                    && m.getStatus() != MovementStatus.CLASSIFICADO) {
                continue;
            }
            if (ehSaldoAnterior(m.getDescricao()) && !opts.incluirSaldoAnterior()) {
                continue;
            }
            String debito = m.getContaDebitoId() == null ? ""
                    : codigoPorConta.getOrDefault(m.getContaDebitoId(), "");
            String credito = m.getContaCreditoId() == null ? ""
                    : codigoPorConta.getOrDefault(m.getContaCreditoId(), "");
            sb.append(String.join(sep,
                    m.getData() == null ? "" : DATA_BR.format(m.getData()),
                    debito,
                    "",                       // reservado (centro de custo debito)
                    credito,
                    "",                       // reservado (centro de custo credito)
                    valorPositivo(m, opts.separadorDecimal()),
                    historico(m.getDescricao()),
                    codigoEmpresa))
              .append('\n');
        }

        String ext = fmt.equals("CSV") ? "csv" : "txt";
        String contentType = fmt.equals("CSV") ? "text/csv" : "text/plain";
        String filename = nomeBase + "." + ext;
        return new ExportFile(filename, contentType, sb.toString().getBytes(resolveCharset(opts.encoding())));
    }

    private ExportOptions withFormato(ExportOptions opts, String formato) {
        if (formato == null || formato.isBlank()) {
            return opts;
        }
        return new ExportOptions(formato, opts.separadorDecimal(), opts.encoding(),
                opts.incluirCabecalho(), opts.somenteConciliados(), opts.incluirSaldoAnterior());
    }

    /** Valor positivo (sem sinal), 2 casas, separador decimal configuravel (padrao virgula). */
    private String valorPositivo(Movement m, char separadorDecimal) {
        BigDecimal v = m.getValor() == null ? BigDecimal.ZERO : m.getValor();
        String s = v.abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
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
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding.trim());
        } catch (RuntimeException ex) {
            return StandardCharsets.UTF_8;
        }
    }
}
