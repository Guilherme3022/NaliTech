package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.account.service.ClassificationSuggestionService;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationRule;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoPendenteEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRuleRepository;
import com.nalitech.shared.util.DescriptionNormalizer;
import com.nalitech.shared.util.StringSimilarity;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conciliacao (o "cerebro" da conciliacao) — casamento <b>simetrico e independente do
 * papel do documento</b>.
 *
 * <p>Cada par conciliado tem exatamente UM item ({@link Reconciliation}): um lado e o
 * "dirigente" ({@code movementId}) e o outro a "contrapartida" ({@code matchedMovementId}).
 * Uma movimentacao pode: dirigir um item, ser contrapartida de um item (status
 * {@link MovementStatus#CONCILIADO}) ou ficar pendente (dirigente sem contrapartida).</p>
 *
 * <p><b>Por que independente de papel:</b> a versao anterior so casava EXTRATO com SISTEMA
 * e exigia que o usuario marcasse cada arquivo. Se ele esquecia (os dois viravam EXTRATO),
 * dava <b>zero match</b>. Agora casamos qualquer movimentacao com a de <b>outro arquivo</b>
 * do mesmo cliente; o papel vira apenas um pequeno <b>bonus</b> no score quando os lados
 * sao opostos. Assim casa mesmo sem marcar os papeis, e continua sem casar linhas do mesmo
 * arquivo entre si.</p>
 *
 * <p><b>Score (0..1):</b> valor (peso {@value #PESO_VALOR}, com tolerancia de
 * {@value #TOLERANCIA_VALOR_PCT} para tarifas/centavos, sinais opostos descartam),
 * data (peso {@value #PESO_DATA}, janela de {@value #JANELA_DIAS} dias) e nome/contraparte
 * (peso {@value #PESO_NOME}, Jaccard + razao). So casa acima de {@value #LIMIAR_MATCH}.</p>
 */
@Slf4j
@Service
@Transactional
public class MatchingService {

    private static final int JANELA_DIAS = 7;
    private static final double TOLERANCIA_VALOR_PCT = 0.02;
    private static final double LIMIAR_MATCH = 0.5;

    private static final double PESO_VALOR = 0.55;
    private static final double PESO_DATA = 0.20;
    private static final double PESO_NOME = 0.25;
    // Bonus quando os papeis sao opostos (extrato x sistema) — reforca a direcao certa
    // sem exigir o papel. Somado ao score final (limitado a 1.0).
    private static final double BONUS_PAPEL_OPOSTO = 0.05;

    private final MovementRepository movementRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationRuleRepository ruleRepository;
    private final ClassificationSuggestionService suggestionService;
    private final ApplicationEventPublisher eventPublisher;

    public MatchingService(MovementRepository movementRepository,
                           ReconciliationRepository reconciliationRepository,
                           ReconciliationRuleRepository ruleRepository,
                           ClassificationSuggestionService suggestionService,
                           ApplicationEventPublisher eventPublisher) {
        this.movementRepository = movementRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.ruleRepository = ruleRepository;
        this.suggestionService = suggestionService;
        this.eventPublisher = eventPublisher;
    }

    public void reconcile(Movement movement) {
        if (movement.getClienteId() == null || movement.getData() == null || movement.getValor() == null) {
            garantirPendente(movement);
            return;
        }
        // Ja e contrapartida de um item (reservada): nada a fazer.
        if (movement.getStatus() == MovementStatus.CONCILIADO) {
            return;
        }
        Optional<Reconciliation> itemProprio = reconciliationRepository.findFirstByMovementId(movement.getId());
        // Ja dirige um item que ja esta casado: nada a fazer.
        if (itemProprio.isPresent() && itemProprio.get().getMatchedMovementId() != null) {
            return;
        }

        // 1) Esta movimentacao consegue FECHAR um item pendente de outra (contrapartida)?
        Reconciliation alvoParaFechar = melhorItemPendentePara(movement);
        if (alvoParaFechar != null) {
            fecharComContrapartida(alvoParaFechar, movement);
            // Se esta movimentacao ja tinha um item proprio ainda sem par, remove
            // (ela agora e contrapartida, nao deve tambem dirigir um item).
            itemProprio.filter(i -> i.getMatchedMovementId() == null)
                    .ifPresent(reconciliationRepository::delete);
            return;
        }

        // 2) Consegue DIRIGIR, reservando uma movimentacao livre de outro arquivo?
        Candidato livre = melhorLivrePara(movement);
        if (livre != null) {
            Movement contrapartida = livre.movimento();
            contrapartida.setStatus(MovementStatus.CONCILIADO); // reserva
            movementRepository.save(contrapartida);

            Reconciliation item = itemProprio.orElseGet(() -> novoItem(movement));
            aplicarMatch(item, movement, contrapartida, livre.nota());
            movement.setStatus(MovementStatus.CONCILIACAO_PENDENTE);
            movementRepository.save(movement);
            gerarSugestaoConta(movement);
            return;
        }

        // 3) Nada casou: aplica regra ou deixa pendente para revisao manual.
        aplicarRegraOuPendente(movement, itemProprio);
    }

    // ---- Passo 1: fechar item pendente de outra movimentacao com ESTA como contrapartida ----
    private Reconciliation melhorItemPendentePara(Movement movement) {
        List<Reconciliation> pendentes = reconciliationRepository
                .findByEmpresaIdAndClienteIdAndStatusAndMatchedMovementIdIsNull(
                        movement.getEmpresaId(), movement.getClienteId(), ReconciliationStatus.PENDENTE);
        Reconciliation melhor = null;
        double melhorNota = -1;
        for (Reconciliation item : pendentes) {
            if (item.getMovementId().equals(movement.getId())) {
                continue; // o proprio item
            }
            Movement dirigente = movementRepository.findById(item.getMovementId()).orElse(null);
            if (dirigente == null || mesmoArquivo(dirigente, movement)) {
                continue;
            }
            double nota = nota(dirigente, movement);
            if (nota >= LIMIAR_MATCH && nota > melhorNota) {
                melhor = item;
                melhorNota = nota;
            }
        }
        if (melhor != null) {
            melhor.setScore(BigDecimal.valueOf(Math.round(melhorNota * 100)));
        }
        return melhor;
    }

    private void fecharComContrapartida(Reconciliation item, Movement contrapartida) {
        Movement dirigente = movementRepository.findById(item.getMovementId()).orElse(null);
        boolean exato = dirigente != null && valorIgual(dirigente, contrapartida)
                && diasEntre(dirigente, contrapartida) == 0;
        item.setMatchedMovementId(contrapartida.getId());
        item.setCamada(exato ? "EXATA" : "APROXIMADA");
        item.setMotivo("Match automatico entre arquivos (confianca "
                + (item.getScore() == null ? "?" : item.getScore().intValue()) + "%)");
        reconciliationRepository.save(item);
        contrapartida.setStatus(MovementStatus.CONCILIADO);
        movementRepository.save(contrapartida);
    }

    // ---- Passo 2: achar a melhor movimentacao livre (NORMALIZADA) de outro arquivo ----
    private Candidato melhorLivrePara(Movement movement) {
        List<Movement> candidatos = movementRepository.findMatchCandidatesInWindow(
                movement.getEmpresaId(), movement.getClienteId(), movement.getUploadId(),
                movement.getData().minusDays(JANELA_DIAS), movement.getData().plusDays(JANELA_DIAS));
        Movement melhor = null;
        double melhorNota = -1;
        for (Movement candidato : candidatos) {
            if (candidato.getId().equals(movement.getId())
                    || reconciliationRepository.existsByMatchedMovementId(candidato.getId())) {
                continue;
            }
            double nota = nota(movement, candidato);
            if (nota >= LIMIAR_MATCH && nota > melhorNota) {
                melhor = candidato;
                melhorNota = nota;
            }
        }
        return melhor == null ? null : new Candidato(melhor, melhorNota);
    }

    private void aplicarMatch(Reconciliation item, Movement dirigente, Movement contrapartida, double nota) {
        boolean exato = valorIgual(dirigente, contrapartida) && diasEntre(dirigente, contrapartida) == 0;
        item.setMatchedMovementId(contrapartida.getId());
        item.setCamada(exato ? "EXATA" : "APROXIMADA");
        item.setScore(BigDecimal.valueOf(Math.round(nota * 100)));
        item.setMotivo(exato
                ? "Match exato entre arquivos (data e valor)"
                : "Match automatico entre arquivos (confianca " + Math.round(nota * 100) + "%)");
        reconciliationRepository.save(item);
    }

    // ---- Passo 3: regra explicita ou pendencia manual ----
    private void aplicarRegraOuPendente(Movement movement, Optional<Reconciliation> itemProprio) {
        Optional<ReconciliationRule> regra = primeiraRegra(movement);
        Reconciliation item = itemProprio.orElseGet(() -> novoItem(movement));
        if (regra.isPresent()) {
            item.setCamada("REGRA");
            item.setScore(BigDecimal.valueOf(80));
            item.setMotivo("Conciliado por regra: " + regra.get().getNome());
        } else {
            item.setCamada("MANUAL");
            item.setScore(BigDecimal.ZERO);
            item.setMotivo("Sem correspondencia automatica: revisao manual");
        }
        item.setMatchedMovementId(null);
        reconciliationRepository.save(item);
        movement.setStatus(MovementStatus.CONCILIACAO_PENDENTE);
        movementRepository.save(movement);
        gerarSugestaoConta(movement);
        eventPublisher.publishEvent(new ConciliacaoPendenteEvent(
                item.getId(), item.getEmpresaId(), movement.getId(), item.getMotivo()));
    }

    private void garantirPendente(Movement movement) {
        if (reconciliationRepository.findFirstByMovementId(movement.getId()).isPresent()) {
            return;
        }
        Reconciliation item = novoItem(movement);
        item.setCamada("MANUAL");
        item.setScore(BigDecimal.ZERO);
        item.setMotivo("Dados insuficientes para conciliacao automatica");
        reconciliationRepository.save(item);
    }

    // ---- Pontuacao ----
    private double nota(Movement a, Movement b) {
        double valorScore = valorScore(a, b);
        if (valorScore < 0) {
            return -1;
        }
        double dataScore = dataScore(a, b);
        if (dataScore < 0) {
            return -1;
        }
        double nomeScore = similaridade(a, b);
        double total = PESO_VALOR * valorScore + PESO_DATA * dataScore + PESO_NOME * nomeScore;
        if (papeisOpostos(a, b)) {
            total = Math.min(1.0, total + BONUS_PAPEL_OPOSTO);
        }
        return total;
    }

    private boolean papeisOpostos(Movement a, Movement b) {
        return a.getOrigem() != null && b.getOrigem() != null
                && !a.getOrigem().equalsIgnoreCase(b.getOrigem());
    }

    private double valorScore(Movement a, Movement b) {
        if (a.getValor() == null || b.getValor() == null) {
            return -1;
        }
        double alvo = a.getValor().doubleValue();
        double cand = b.getValor().doubleValue();
        double base = Math.max(Math.abs(alvo), 0.01);
        double ratio = Math.abs(alvo - cand) / base; // sinais opostos -> ratio grande -> descarta
        if (ratio > TOLERANCIA_VALOR_PCT) {
            return -1;
        }
        return 1.0 - (ratio / TOLERANCIA_VALOR_PCT);
    }

    private double dataScore(Movement a, Movement b) {
        long dd = diasEntre(a, b);
        if (dd > JANELA_DIAS) {
            return -1;
        }
        return 1.0 - ((double) dd / JANELA_DIAS);
    }

    private boolean valorIgual(Movement a, Movement b) {
        return a.getValor() != null && b.getValor() != null
                && a.getValor().compareTo(b.getValor()) == 0;
    }

    private long diasEntre(Movement a, Movement b) {
        return Math.abs(ChronoUnit.DAYS.between(a.getData(), b.getData()));
    }

    private boolean mesmoArquivo(Movement a, Movement b) {
        return a.getUploadId() != null && a.getUploadId().equals(b.getUploadId());
    }

    private double similaridade(Movement a, Movement b) {
        String na = DescriptionNormalizer.normalize(a.getDescricao());
        String nb = DescriptionNormalizer.normalize(b.getDescricao());
        if (na.isBlank() || nb.isBlank()) {
            return 0.0;
        }
        return Math.max(StringSimilarity.tokenSimilarity(na, nb), StringSimilarity.ratio(na, nb));
    }

    private Optional<ReconciliationRule> primeiraRegra(Movement movement) {
        List<ReconciliationRule> rules = ruleRepository.findByEmpresaIdAndAtivoTrue(movement.getEmpresaId());
        return rules.stream().filter(rule -> ruleMatches(rule, movement)).findFirst();
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

    private void gerarSugestaoConta(Movement movement) {
        try {
            suggestionService.suggestDeterministic(movement);
        } catch (RuntimeException ex) {
            log.warn("Sugestao de conta ignorada para movimentacao {}: {}",
                    movement.getId(), ex.getMessage());
        }
    }

    private Reconciliation novoItem(Movement movement) {
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setEmpresaId(movement.getEmpresaId());
        reconciliation.setClienteId(movement.getClienteId());
        reconciliation.setCompetencia(
                movement.getData() != null ? movement.getData().withDayOfMonth(1) : null);
        reconciliation.setMovementId(movement.getId());
        reconciliation.setStatus(ReconciliationStatus.PENDENTE);
        return reconciliation;
    }

    private record Candidato(Movement movimento, double nota) {
    }
}
