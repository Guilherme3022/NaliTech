package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationRule;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoPendenteEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRuleRepository;
import com.nalitech.shared.util.StringSimilarity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conciliacao extrato x sistema.
 *
 * <p>Modelo: cada item de conciliacao ({@link Reconciliation}) e dirigido por uma linha
 * de <b>EXTRATO</b> (lado banco) e casado com a melhor movimentacao do <b>SISTEMA</b>
 * (contas a pagar/receber). Movimentacoes de origem SISTEMA NAO geram item proprio —
 * elas preenchem itens de extrato pendentes. Assim a ordem de upload nao importa:
 * se o extrato chega primeiro, fica pendente e e preenchido quando o sistema chega
 * (e vice-versa).</p>
 *
 * <p>Criterio de match: mesmo <b>valor com sinal</b> (pagamento no banco = -X casa com
 * conta a pagar = -X), <b>data dentro de uma janela</b> (data do banco e a de pagamento
 * do sistema costumam diferir alguns dias) e <b>similaridade de descricao/contraparte</b>
 * como desempate.</p>
 */
@Service
@Transactional
public class MatchingService {

    // Janela de dias entre a data do extrato e a data do lancamento no sistema.
    private static final int JANELA_DIAS = 5;

    private final MovementRepository movementRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationRuleRepository ruleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchingService(MovementRepository movementRepository,
                           ReconciliationRepository reconciliationRepository,
                           ReconciliationRuleRepository ruleRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.movementRepository = movementRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.ruleRepository = ruleRepository;
        this.eventPublisher = eventPublisher;
    }

    public void reconcile(Movement movement) {
        // Lado sistema: nao cria item proprio; tenta preencher um item de extrato pendente.
        if (isSistema(movement)) {
            preencherExtratoPendente(movement);
            return;
        }

        // Lado extrato: cria/atualiza o item de conciliacao.
        Reconciliation result = matchSistema(movement)
                .or(() -> matchByRules(movement))
                .orElseGet(() -> pendingWithoutMatch(movement));

        movement.setStatus(MovementStatus.CONCILIACAO_PENDENTE);
        movementRepository.save(movement);
        reconciliationRepository.save(result);

        if (result.getMatchedMovementId() == null) {
            eventPublisher.publishEvent(new ConciliacaoPendenteEvent(
                    result.getId(), result.getEmpresaId(), movement.getId(), result.getMotivo()));
        }
    }

    private boolean isSistema(Movement movement) {
        return "SISTEMA".equalsIgnoreCase(movement.getOrigem());
    }

    /** Busca no lado sistema a melhor movimentacao para esta linha de extrato. */
    private Optional<Reconciliation> matchSistema(Movement extrato) {
        if (extrato.getClienteId() == null || extrato.getData() == null || extrato.getValor() == null) {
            return Optional.empty();
        }
        List<Movement> candidatos = movementRepository.findSistemaCandidates(
                extrato.getEmpresaId(), extrato.getClienteId(), extrato.getValor(),
                extrato.getData().minusDays(JANELA_DIAS), extrato.getData().plusDays(JANELA_DIAS));

        Movement melhor = null;
        double melhorSim = -1;
        long melhorDiff = Long.MAX_VALUE;
        for (Movement candidato : candidatos) {
            // Nao reaproveita um lancamento do sistema ja casado com outro extrato.
            if (reconciliationRepository.existsByMatchedMovementId(candidato.getId())) {
                continue;
            }
            double sim = similaridade(extrato, candidato);
            long diff = diasEntre(extrato, candidato);
            if (sim > melhorSim || (sim == melhorSim && diff < melhorDiff)) {
                melhor = candidato;
                melhorSim = sim;
                melhorDiff = diff;
            }
        }
        if (melhor == null) {
            return Optional.empty();
        }
        melhor.setStatus(MovementStatus.CONCILIADO); // reserva o lancamento do sistema
        movementRepository.save(melhor);

        String camada = melhorDiff == 0 ? "EXATA" : "APROXIMADA";
        // Valor ja confere (100%); a similaridade da contraparte ajusta a confianca.
        BigDecimal score = BigDecimal.valueOf(Math.round((0.6 + 0.4 * Math.max(0, melhorSim)) * 100));
        String motivo = melhorDiff == 0
                ? "Match exato extrato x sistema (data e valor)"
                : "Match aproximado extrato x sistema (valor confere, data +/-" + melhorDiff + "d)";
        return Optional.of(build(extrato, melhor.getId(), camada, score, motivo));
    }

    /** Quando uma movimentacao do sistema chega, tenta fechar um item de extrato pendente. */
    private void preencherExtratoPendente(Movement sistema) {
        if (sistema.getClienteId() == null || sistema.getData() == null || sistema.getValor() == null) {
            return;
        }
        List<Reconciliation> pendentes = reconciliationRepository
                .findByEmpresaIdAndClienteIdAndStatusAndMatchedMovementIdIsNull(
                        sistema.getEmpresaId(), sistema.getClienteId(), ReconciliationStatus.PENDENTE);

        Reconciliation melhorItem = null;
        double melhorSim = -1;
        long melhorDiff = Long.MAX_VALUE;
        for (Reconciliation item : pendentes) {
            Movement extrato = movementRepository.findById(item.getMovementId()).orElse(null);
            if (extrato == null || extrato.getValor() == null || extrato.getData() == null) {
                continue;
            }
            if (extrato.getValor().compareTo(sistema.getValor()) != 0) {
                continue;
            }
            long diff = diasEntre(extrato, sistema);
            if (diff > JANELA_DIAS) {
                continue;
            }
            double sim = similaridade(extrato, sistema);
            if (sim > melhorSim || (sim == melhorSim && diff < melhorDiff)) {
                melhorItem = item;
                melhorSim = sim;
                melhorDiff = diff;
            }
        }
        if (melhorItem == null) {
            return;
        }
        melhorItem.setMatchedMovementId(sistema.getId());
        melhorItem.setCamada(melhorDiff == 0 ? "EXATA" : "APROXIMADA");
        melhorItem.setScore(BigDecimal.valueOf(Math.round((0.6 + 0.4 * Math.max(0, melhorSim)) * 100)));
        melhorItem.setMotivo("Match extrato x sistema (lado sistema chegou depois)");
        reconciliationRepository.save(melhorItem);
        sistema.setStatus(MovementStatus.CONCILIADO);
        movementRepository.save(sistema);
    }

    private Optional<Reconciliation> matchByRules(Movement movement) {
        List<ReconciliationRule> rules = ruleRepository.findByEmpresaIdAndAtivoTrue(movement.getEmpresaId());
        for (ReconciliationRule rule : rules) {
            if (ruleMatches(rule, movement)) {
                return Optional.of(build(movement, null, "REGRA",
                        BigDecimal.valueOf(80), "Conciliado por regra: " + rule.getNome()));
            }
        }
        return Optional.empty();
    }

    private boolean ruleMatches(ReconciliationRule rule, Movement movement) {
        boolean descricaoOk = rule.getDescricaoContains() == null
                || (movement.getDescricao() != null
                    && movement.getDescricao().toLowerCase()
                        .contains(rule.getDescricaoContains().toLowerCase()));
        boolean valorOk = rule.getValorMin() == null
                || (movement.getValor() != null
                    && movement.getValor().abs().compareTo(rule.getValorMin()) >= 0);
        return descricaoOk && valorOk;
    }

    private Reconciliation pendingWithoutMatch(Movement movement) {
        return build(movement, null, "MANUAL", BigDecimal.ZERO,
                "Sem correspondencia automatica no sistema: revisao manual");
    }

    private long diasEntre(Movement a, Movement b) {
        return Math.abs(ChronoUnit.DAYS.between(a.getData(), b.getData()));
    }

    // Similaridade da contraparte: normaliza a descricao (tira prefixos genericos e
    // numeros) para comparar os nomes/CNPJ das partes, nao o tipo de operacao.
    private double similaridade(Movement a, Movement b) {
        return StringSimilarity.ratio(normalizar(a.getDescricao()), normalizar(b.getDescricao()));
    }

    private String normalizar(String descricao) {
        if (descricao == null) {
            return "";
        }
        String texto = descricao.toLowerCase();
        // Remove prefixos de tipo de operacao que nao ajudam a identificar a contraparte.
        texto = texto.replaceAll(
                "\\b(pix|ted|doc|tef|pagamento|recebido|recebimento|boleto|cred|deb|"
                + "credito|debito|transferencia|cartao|debito|liquid|princ|de|da|do|ltda|sa|s/a|me|epp)\\b",
                " ");
        texto = texto.replaceAll("[0-9]", " ").replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        return texto;
    }

    private Reconciliation build(Movement movement, UUID matchedId, String camada,
                                 BigDecimal score, String motivo) {
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setEmpresaId(movement.getEmpresaId());
        // EA: propaga cliente e competencia (1o dia do mes da movimentacao).
        reconciliation.setClienteId(movement.getClienteId());
        reconciliation.setCompetencia(
                movement.getData() != null ? movement.getData().withDayOfMonth(1) : null);
        reconciliation.setMovementId(movement.getId());
        reconciliation.setMatchedMovementId(matchedId);
        reconciliation.setStatus(ReconciliationStatus.PENDENTE);
        reconciliation.setCamada(camada);
        reconciliation.setScore(score);
        reconciliation.setMotivo(motivo);
        return reconciliation;
    }
}
