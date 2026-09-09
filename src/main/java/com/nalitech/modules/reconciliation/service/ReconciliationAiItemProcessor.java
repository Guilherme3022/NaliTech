package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher;
import com.nalitech.modules.reconciliation.ai.AiReconciliationMatcher.AiMatch;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa UM item da varredura por IA numa transacao propria, para que o
 * progresso do sweep seja duravel e a falha de um item nao derrube os demais.
 *
 * <p>Autossuficiente: busca os candidatos plausiveis (mesma janela de matching,
 * porem mais larga), consulta o LLM e, quando a confianca e suficiente, casa o
 * item (camada IA) reservando a contrapartida — espelhando o que o
 * {@code MatchingService} faz no fluxo automatico. Sempre marca {@code iaTentada}
 * (memoria: nao chamar a IA de novo para o mesmo item).</p>
 */
@Service
public class ReconciliationAiItemProcessor {

    // Janela de datas para a IA buscar contrapartes (mais larga que o match automatico).
    private static final int JANELA_DIAS_IA = 21;

    private final ReconciliationRepository reconciliationRepository;
    private final MovementRepository movementRepository;
    private final AiReconciliationMatcher aiMatcher;
    private final int maxCandidates;
    private final BigDecimal minConfidence;

    public ReconciliationAiItemProcessor(
            ReconciliationRepository reconciliationRepository,
            MovementRepository movementRepository,
            AiReconciliationMatcher aiMatcher,
            @Value("${reconciliation.match.ai-max-candidates:20}") int maxCandidates,
            @Value("${reconciliation.match.ai-min-confidence:70}") BigDecimal minConfidence) {
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
        this.aiMatcher = aiMatcher;
        this.maxCandidates = maxCandidates;
        this.minConfidence = minConfidence;
    }

    /**
     * Avalia uma pendencia MANUAL com a IA. Marca {@code iaTentada=true} de todo
     * jeito (nao chamar de novo). Devolve true se a IA conciliou o item.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(UUID reconciliationId, UUID empresaId) {
        Reconciliation r = reconciliationRepository.findByIdAndEmpresaId(reconciliationId, empresaId)
                .orElse(null);
        if (r == null || r.getStatus() != ReconciliationStatus.PENDENTE
                || !"MANUAL".equals(r.getCamada()) || r.isIaTentada()) {
            return false;
        }
        Movement movement = movementRepository.findById(r.getMovementId()).orElse(null);
        if (movement == null || movement.getClienteId() == null
                || movement.getData() == null || movement.getValor() == null) {
            marcarTentada(r);
            return false;
        }

        List<Movement> candidatos = candidatos(movement);
        if (candidatos.isEmpty()) {
            marcarTentada(r);
            return false;
        }

        Optional<AiMatch> aiMatch;
        try {
            aiMatch = aiMatcher.findMatch(movement, candidatos)
                    .filter(m -> m.confianca().compareTo(minConfidence) >= 0);
        } catch (RuntimeException ex) {
            // IA indisponivel (rate limit 429 / rede): NAO marca como tentado, para
            // reprocessar no proximo sweep quando o limite do free tier resetar.
            return false;
        }

        boolean resolvido = false;
        if (aiMatch.isPresent()) {
            AiMatch match = aiMatch.get();
            Movement contrapartida = movementRepository.findById(match.matchedMovementId()).orElse(null);
            // So aplica se a contrapartida ainda estiver livre (nao reservada por outro item).
            if (contrapartida != null
                    && !reconciliationRepository.existsByMatchedMovementId(contrapartida.getId())) {
                r.setMatchedMovementId(contrapartida.getId());
                r.setCamada("IA");
                r.setScore(match.confianca());
                r.setMotivo("IA: " + match.justificativa());
                contrapartida.setStatus(MovementStatus.CONCILIADO); // reserva
                movementRepository.save(contrapartida);
                resolvido = true;
            }
        }
        r.setIaTentada(true);
        reconciliationRepository.save(r);
        return resolvido;
    }

    // Candidatos plausiveis: movimentacoes de outro arquivo na janela de datas,
    // ainda nao reservadas como contrapartida, limitadas para conter custo/latencia.
    private List<Movement> candidatos(Movement movement) {
        LocalDate inicio = movement.getData().minusDays(JANELA_DIAS_IA);
        LocalDate fim = movement.getData().plusDays(JANELA_DIAS_IA);
        List<Movement> encontrados = movementRepository.findMatchCandidatesInWindow(
                movement.getEmpresaId(), movement.getClienteId(), movement.getUploadId(), inicio, fim);
        List<Movement> filtrados = new ArrayList<>();
        for (Movement c : encontrados) {
            if (!c.getId().equals(movement.getId())
                    && !reconciliationRepository.existsByMatchedMovementId(c.getId())) {
                filtrados.add(c);
                if (filtrados.size() >= maxCandidates) {
                    break;
                }
            }
        }
        return filtrados;
    }

    private void marcarTentada(Reconciliation r) {
        r.setIaTentada(true);
        reconciliationRepository.save(r);
    }
}
