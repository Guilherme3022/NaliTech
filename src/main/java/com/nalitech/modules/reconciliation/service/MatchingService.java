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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchingService {

    private static final double SIMILARITY_THRESHOLD = 0.7;

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

    public Reconciliation reconcile(Movement movement) {
        Reconciliation result = matchExact(movement)
                .or(() -> matchBySimilarity(movement))
                .or(() -> matchByRules(movement))
                .orElseGet(() -> pendingWithoutMatch(movement));

        movement.setStatus(MovementStatus.CONCILIACAO_PENDENTE);
        movementRepository.save(movement);
        Reconciliation saved = reconciliationRepository.save(result);

        if (saved.getMatchedMovementId() == null) {
            eventPublisher.publishEvent(new ConciliacaoPendenteEvent(
                    saved.getId(), saved.getEmpresaId(), movement.getId(), saved.getMotivo()));
        }
        return saved;
    }

    private Optional<Reconciliation> matchExact(Movement movement) {
        if (movement.getData() == null || movement.getValor() == null) {
            return Optional.empty();
        }
        return movementRepository
                .findByEmpresaIdAndDataAndValor(movement.getEmpresaId(), movement.getData(), movement.getValor())
                .stream()
                .filter(candidate -> !candidate.getId().equals(movement.getId()))
                .findFirst()
                .map(candidate -> build(movement, candidate.getId(), "EXATA",
                        BigDecimal.valueOf(100), "Match exato por data e valor"));
    }

    private Optional<Reconciliation> matchBySimilarity(Movement movement) {
        if (movement.getValor() == null) {
            return Optional.empty();
        }
        Movement best = null;
        double bestScore = 0;
        for (Movement candidate : movementRepository.findByEmpresaIdAndValor(
                movement.getEmpresaId(), movement.getValor())) {
            if (candidate.getId().equals(movement.getId())) {
                continue;
            }
            double score = StringSimilarity.ratio(movement.getDescricao(), candidate.getDescricao());
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null && bestScore >= SIMILARITY_THRESHOLD) {
            return Optional.of(build(movement, best.getId(), "SIMILARIDADE",
                    BigDecimal.valueOf(bestScore * 100), "Match por valor e descricao semelhante"));
        }
        return Optional.empty();
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
                "Sem correspondencia automatica: revisao manual");
    }

    private Reconciliation build(Movement movement, UUID matchedId, String camada,
                                 BigDecimal score, String motivo) {
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setEmpresaId(movement.getEmpresaId());
        reconciliation.setMovementId(movement.getId());
        reconciliation.setMatchedMovementId(matchedId);
        reconciliation.setStatus(ReconciliationStatus.PENDENTE);
        reconciliation.setCamada(camada);
        reconciliation.setScore(score);
        reconciliation.setMotivo(motivo);
        return reconciliation;
    }
}
