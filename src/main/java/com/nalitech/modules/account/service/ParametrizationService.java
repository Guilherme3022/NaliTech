package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.AccountDtos.ApplyParametrizationRequest;
import com.nalitech.modules.account.dto.AccountDtos.ApplyParametrizationResponse;
import com.nalitech.modules.account.dto.AccountDtos.ParametrizationRequest;
import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.repository.AccountRuleRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parametrizacao "De/Para": lista os padroes que ainda nao tem conta contabil
 * vinculada (fila de "Solicitacao de Parametrizacao") e permite aplicar um
 * De/Para em lote a todas as movimentacoes pendentes que casam com um termo.
 */
@Service
@Transactional
public class ParametrizationService {

    private static final int MAX_REQUESTS = 200;
    private static final int MAX_PATTERN_LENGTH = 200;

    private final MovementRepository movementRepository;
    private final ClassificationService classificationService;
    private final AccountRuleRepository accountRuleRepository;

    public ParametrizationService(MovementRepository movementRepository,
                                  ClassificationService classificationService,
                                  AccountRuleRepository accountRuleRepository) {
        this.movementRepository = movementRepository;
        this.classificationService = classificationService;
        this.accountRuleRepository = accountRuleRepository;
    }

    /** Padroes conciliados mas ainda sem De/Para, agrupados por descricao normalizada. */
    @Transactional(readOnly = true)
    public List<ParametrizationRequest> pendingRequests() {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Map<String, Aggregate> agrupado = new LinkedHashMap<>();

        for (Movement movement : movementRepository
                .findByEmpresaIdAndStatusAndCategoriaSugeridaIsNull(empresaId, MovementStatus.CONCILIADO)) {
            String padrao = normalize(movement.getDescricao());
            if (padrao.isEmpty()) {
                continue;
            }
            Aggregate agg = agrupado.computeIfAbsent(padrao, key -> new Aggregate(movement.getDescricao()));
            agg.ocorrencias++;
            if (movement.getValor() != null) {
                agg.valorTotal = agg.valorTotal.add(movement.getValor().abs());
            }
        }

        return agrupado.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, Aggregate> e) -> e.getValue().ocorrencias)
                        .reversed())
                .limit(MAX_REQUESTS)
                .map(e -> new ParametrizationRequest(
                        e.getKey(), e.getValue().exemplo, e.getValue().ocorrencias, e.getValue().valorTotal))
                .toList();
    }

    /** Aplica o De/Para: classifica em lote e (opcional) cria uma regra permanente. */
    public ApplyParametrizationResponse apply(ApplyParametrizationRequest request) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        String termo = request.descricaoContains() == null ? "" : request.descricaoContains().trim();
        if (termo.isEmpty() || request.contaId() == null) {
            throw new BusinessException("Informe o termo e a conta contabil.", HttpStatus.BAD_REQUEST);
        }

        List<Movement> pendentes = movementRepository
                .findPendingByDescricaoContains(empresaId, MovementStatus.CONCILIADO, termo);
        for (Movement movement : pendentes) {
            classificationService.classify(movement.getId(), request.contaId());
        }

        boolean regraCriada = false;
        if (request.criarRegra()) {
            AccountRule rule = new AccountRule();
            rule.setEmpresaId(empresaId);
            rule.setNome("De/Para: " + termo);
            rule.setDescricaoContains(termo);
            rule.setContaId(request.contaId());
            rule.setAtivo(true);
            accountRuleRepository.save(rule);
            regraCriada = true;
        }

        return new ApplyParametrizationResponse(pendentes.size(), regraCriada);
    }

    private String normalize(String descricao) {
        if (descricao == null) {
            return "";
        }
        String padrao = descricao.toLowerCase().replaceAll("\\s+", " ").trim();
        return padrao.length() > MAX_PATTERN_LENGTH ? padrao.substring(0, MAX_PATTERN_LENGTH) : padrao;
    }

    private static final class Aggregate {
        private final String exemplo;
        private long ocorrencias;
        private BigDecimal valorTotal = BigDecimal.ZERO;

        private Aggregate(String exemplo) {
            this.exemplo = exemplo;
        }
    }
}
