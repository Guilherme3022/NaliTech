package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Conciliacao;
import com.nalitech.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF (spec secoes 15-16): gera o arquivo de resultado de uma conciliacao
 * concluida. Formatos no MVP: TXT (principal p/ sistema contabil) e CSV.
 */
@Service
public class ConciliacaoExportService {

    public record ExportFile(String filename, String contentType, byte[] content) {
    }

    private final ConciliacaoService conciliacaoService;
    private final MovementRepository movementRepository;

    public ConciliacaoExportService(ConciliacaoService conciliacaoService,
                                    MovementRepository movementRepository) {
        this.conciliacaoService = conciliacaoService;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public ExportFile export(UUID conciliacaoId, String formato) {
        Conciliacao conciliacao = conciliacaoService.requireConcluida(conciliacaoId);
        String fmt = formato == null ? "TXT" : formato.trim().toUpperCase();

        LocalDate inicio = conciliacao.getCompetencia();
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        List<Movement> movimentos = movementRepository
                .findByEmpresaIdAndClienteIdAndDataBetweenOrderByData(
                        conciliacao.getEmpresaId(), conciliacao.getClienteId(), inicio, fim);

        String sep = switch (fmt) {
            case "CSV" -> ";";
            case "TXT" -> "\t";
            default -> throw new BusinessException(
                    "Formato nao suportado: " + fmt + " (use TXT ou CSV).", HttpStatus.BAD_REQUEST);
        };

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(sep, "data", "valor", "descricao", "conta_debito", "conta_credito"))
          .append('\n');
        for (Movement m : movimentos) {
            sb.append(String.join(sep,
                    nvl(m.getData()),
                    nvl(m.getValor()),
                    sanitize(m.getDescricao(), sep),
                    nvl(m.getContaDebitoId()),
                    nvl(m.getContaCreditoId())))
              .append('\n');
        }

        String ext = fmt.equals("CSV") ? "csv" : "txt";
        String contentType = fmt.equals("CSV") ? "text/csv" : "text/plain";
        String filename = "conciliacao-" + conciliacaoId.toString().substring(0, 8) + "." + ext;
        return new ExportFile(filename, contentType, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String nvl(Object value) {
        return value == null ? "" : value.toString();
    }

    private String sanitize(String value, String sep) {
        if (value == null) {
            return "";
        }
        return value.replace(sep, " ").replace("\n", " ").replace("\r", " ");
    }
}
