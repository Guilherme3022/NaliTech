package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher.AiMatch;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationRule;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoPendenteEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRuleRepository;
import com.nalitech.shared.util.DescriptionNormalizer;
import com.nalitech.shared.util.StringSimilarity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de conciliacao em camadas (cascata): a primeira camada que encontrar um
 * bom candidato vence. Ordem:
 * <ol>
 *   <li><b>EXATA</b> — mesma data e mesmo valor.</li>
 *   <li><b>SIMILARIDADE</b> — mesmo valor + descricao semelhante (Jaccard por tokens).</li>
 *   <li><b>REGRA</b> — regras de conciliacao configuradas.</li>
 *   <li><b>APROXIMADA</b> — valor dentro de uma tolerancia (centavos) e data dentro
 *       de uma janela (compensacao D+n), com descricao razoavelmente parecida.</li>
 *   <li><b>IA</b> — validador por LLM sobre os candidatos plausiveis (opcional, sem
 *       custo com Ollama/Groq).</li>
 *   <li><b>MANUAL</b> — sem correspondencia; revisao humana.</li>
 * </ol>
 * Em todos os casos o automatico apenas sugere; a confirmacao e humana.
 */
@Service
@Transactional
public class MatchingService {

    private final MovementRepository movementRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationRuleRepository ruleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AiReconciliationMatcher aiMatcher;

    private final int dateWindowDays;
    private final BigDecimal valueTolerance;
    private final double similarityThreshold;
    private final double approxDescThreshold;
    private final BigDecimal aiMinConfidence;
    private final int aiMaxCandidates;
    private final boolean aiInline;

    public MatchingService(MovementRepository movementRepository,
                           ReconciliationRepository reconciliationRepository,
                           ReconciliationRuleRepository ruleRepository,
                           ApplicationEventPublisher eventPublisher,
                           AiReconciliationMatcher aiMatcher,
                           @Value("${reconciliation.match.date-window-days:3}") int dateWindowDays,
                           @Value("${reconciliation.match.value-tolerance:0.02}") BigDecimal valueTolerance,
                           @Value("${reconciliation.match.similarity-threshold:0.7}") double similarityThreshold,
                           @Value("${reconciliation.match.approx-desc-threshold:0.5}") double approxDescThreshold,
                           @Value("${reconciliation.match.ai-min-confidence:70}") BigDecimal aiMinConfidence,
                           @Value("${reconciliation.match.ai-max-candidates:20}") int aiMaxCandidates,
                           @Value("${reconciliation.match.ai-inline:false}") boolean aiInline) {
        this.movementRepository = movementRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.ruleRepository = ruleRepository;
        this.eventPublisher = eventPublisher;
        this.aiMatcher = aiMatcher;
        this.dateWindowDays = dateWindowDays;
        this.valueTolerance = valueTolerance;
        this.similarityThreshold = similarityThreshold;
        this.approxDescThreshold = approxDescThreshold;
        this.aiMinConfidence = aiMinConfidence;
        this.aiMaxCandidates = aiMaxCandidates;
        this.aiInline = aiInline;
    }

    public Reconciliation reconcile(Movement movement) {
        // Por padrao a IA NAO roda no pipeline automatico (e lenta): fica para o
        // sweep sob demanda (botao). Ligue reconciliation.match.ai-inline=true para
        // acionar a IA ja na conciliacao automatica.
        Reconciliation result = matchExact(movement)
                .or(() -> matchBySimilarity(movement))
                .or(() -> matchByRules(movement))
                .or(() -> matchApproximate(movement))
                .or(() -> aiInline ? matchByAi(movement) : Optional.empty())
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
        String alvo = DescriptionNormalizer.normalize(movement.getDescricao());
        Movement best = null;
        double bestScore = 0;
        for (Movement candidate : movementRepository.findByEmpresaIdAndValor(
                movement.getEmpresaId(), movement.getValor())) {
            if (candidate.getId().equals(movement.getId())) {
                continue;
            }
            // Jaccard por tokens (mais robusto que Levenshtein para descricoes bancarias).
            double score = StringSimilarity.tokenSimilarity(
                    alvo, DescriptionNormalizer.normalize(candidate.getDescricao()));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null && bestScore >= similarityThreshold) {
            return Optional.of(build(movement, best.getId(), "SIMILARIDADE",
                    BigDecimal.valueOf(Math.round(bestScore * 100)),
                    "Match por valor e descricao semelhante"));
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

    /**
     * Match aproximado: tolera diferenca de centavos no valor e de alguns dias na
     * data (compensacao D+n), exigindo descricao razoavelmente parecida.
     */
    private Optional<Reconciliation> matchApproximate(Movement movement) {
        List<Movement> candidatos = candidates(movement, dateWindowDays);
        if (candidatos.isEmpty()) {
            return Optional.empty();
        }
        String alvo = DescriptionNormalizer.normalize(movement.getDescricao());
        Movement best = null;
        double bestScore = 0;
        for (Movement candidate : candidatos) {
            double score = StringSimilarity.tokenSimilarity(
                    alvo, DescriptionNormalizer.normalize(candidate.getDescricao()));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null && bestScore >= approxDescThreshold) {
            // Score conservador: 60..90 conforme a semelhanca da descricao.
            long score = Math.round(60 + bestScore * 30);
            return Optional.of(build(movement, best.getId(), "APROXIMADA",
                    BigDecimal.valueOf(score),
                    "Match aproximado (tolerancia de valor/data) por descricao semelhante"));
        }
        return Optional.empty();
    }

    /** Camada de IA: valida os candidatos plausiveis via LLM (opcional, sem custo com Ollama/Groq). */
    private Optional<Reconciliation> matchByAi(Movement movement) {
        return evaluateWithAi(movement)
                .map(match -> build(movement, match.matchedMovementId(), "IA",
                        match.confianca(), "IA: " + match.justificativa()));
    }

    /**
     * Avalia a movimentacao com a IA sobre os candidatos plausiveis. Reutilizado
     * pelo sweep sob demanda. Devolve vazio se a IA estiver desligada, nao houver
     * candidatos, a IA nao identificar match, ou a confianca ficar abaixo do minimo.
     */
    public Optional<AiMatch> evaluateWithAi(Movement movement) {
        if (!aiMatcher.isEnabled()) {
            return Optional.empty();
        }
        // Janela mais larga para dar opcoes a IA, limitada para conter custo/latencia.
        List<Movement> candidatos = candidates(movement, dateWindowDays * 3);
        if (candidatos.size() > aiMaxCandidates) {
            candidatos = candidatos.subList(0, aiMaxCandidates);
        }
        if (candidatos.isEmpty()) {
            return Optional.empty();
        }
        return aiMatcher.findMatch(movement, candidatos)
                .filter(match -> match.confianca().compareTo(aiMinConfidence) >= 0);
    }

    // Busca candidatos por faixa de valor (tolerancia de centavos) e janela de datas.
    private List<Movement> candidates(Movement movement, int windowDays) {
        if (movement.getValor() == null || movement.getData() == null) {
            return List.of();
        }
        BigDecimal valorMin = movement.getValor().subtract(valueTolerance);
        BigDecimal valorMax = movement.getValor().add(valueTolerance);
        LocalDate inicio = movement.getData().minusDays(windowDays);
        LocalDate fim = movement.getData().plusDays(windowDays);
        List<Movement> encontrados = movementRepository.findReconciliationCandidates(
                movement.getEmpresaId(), valorMin, valorMax, inicio, fim);
        List<Movement> filtrados = new ArrayList<>(encontrados.size());
        for (Movement candidate : encontrados) {
            if (!candidate.getId().equals(movement.getId())) {
                filtrados.add(candidate);
            }
        }
        return filtrados;
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
