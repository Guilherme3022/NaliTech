package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.account.entity.AiSuggestion;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.AiSuggestionRepository;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.BatchConfirmItem;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.MovementView;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationResponse;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.ReconciliationSummary;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.SugestaoView;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.SummaryLine;
import com.nalitech.modules.reconciliation.dto.ReconciliationDtos.GroupMatchRequest;
import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationMatch;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import com.nalitech.modules.reconciliation.event.ConciliacaoEvents.ConciliacaoConfirmadaEvent;
import com.nalitech.modules.reconciliation.repository.ReconciliationMatchRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final MovementRepository movementRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final ReconciliationMatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReconciliationService(ReconciliationRepository reconciliationRepository,
                                 MovementRepository movementRepository,
                                 ChartOfAccountRepository chartOfAccountRepository,
                                 AiSuggestionRepository aiSuggestionRepository,
                                 ReconciliationMatchRepository matchRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.movementRepository = movementRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.aiSuggestionRepository = aiSuggestionRepository;
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationResponse> pending(UUID clienteId, LocalDate competencia, Pageable pageable) {
        Page<Reconciliation> page = reconciliationRepository
                .search(SecurityUtils.currentEmpresaId(), ReconciliationStatus.PENDENTE,
                        clienteId, competencia, pageable);
        return new PageImpl<>(buildResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationResponse> history(ReconciliationStatus status, UUID clienteId,
                                                LocalDate competencia, Pageable pageable) {
        Page<Reconciliation> page = reconciliationRepository
                .search(SecurityUtils.currentEmpresaId(), status, clienteId, competencia, pageable);
        return new PageImpl<>(buildResponses(page.getContent()), pageable, page.getTotalElements());
    }

    public ReconciliationResponse confirm(UUID id, UUID contaSugerida) {
        Reconciliation reconciliation = findPending(id);
        requirePlanoDeContas(reconciliation);
        requireContaLancavel(reconciliation, contaSugerida);
        reconciliation.setStatus(ReconciliationStatus.CONFIRMADO);
        reconciliationRepository.save(reconciliation);

        updateMovementStatus(reconciliation.getMovementId(), MovementStatus.CONCILIADO);

        eventPublisher.publishEvent(new ConciliacaoConfirmadaEvent(
                reconciliation.getId(), reconciliation.getEmpresaId(),
                reconciliation.getMovementId(), contaSugerida));
        return toResponse(reconciliation);
    }

    public ReconciliationResponse reject(UUID id) {
        Reconciliation reconciliation = findPending(id);
        reconciliation.setStatus(ReconciliationStatus.REJEITADO);
        reconciliationRepository.save(reconciliation);
        updateMovementStatus(reconciliation.getMovementId(), MovementStatus.NORMALIZADO);
        return toResponse(reconciliation);
    }

    /** Confirma varios itens de uma vez (acao em lote). Cada item pode trazer sua conta. */
    public List<ReconciliationResponse> confirmMany(List<BatchConfirmItem> itens) {
        List<ReconciliationResponse> result = new ArrayList<>(itens.size());
        for (BatchConfirmItem item : itens) {
            result.add(confirm(item.id(), item.contaSugerida()));
        }
        return result;
    }

    /** Rejeita varios itens de uma vez (acao em lote). */
    public List<ReconciliationResponse> rejectMany(List<UUID> ids) {
        List<ReconciliationResponse> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            result.add(reject(id));
        }
        return result;
    }

    /**
     * Pareamento N:1 (agrupamento): casa o lancamento do extrato (movimento principal desta
     * conciliacao) com varias movimentacoes do sistema cuja soma bate com o valor do extrato.
     * Ex.: um deposito unico que quita varias duplicatas. So agrupa se a soma conferir (a menos
     * de 1 centavo), evitando conciliacao errada.
     */
    public ReconciliationResponse groupMatch(UUID id, GroupMatchRequest request) {
        Reconciliation reconciliation = findPending(id);
        UUID empresaId = reconciliation.getEmpresaId();
        List<UUID> movementIds = request.movementIds();

        Movement principal = movementRepository
                .findByIdAndEmpresaId(reconciliation.getMovementId(), empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao principal nao encontrada."));
        BigDecimal alvo = principal.getValor() == null ? BigDecimal.ZERO : principal.getValor().abs();

        List<Movement> pernas = new ArrayList<>(movementIds.size());
        BigDecimal soma = BigDecimal.ZERO;
        for (UUID movementId : movementIds) {
            if (movementId.equals(principal.getId())) {
                throw new BusinessException(
                        "Nao e possivel agrupar a movimentacao do extrato com ela mesma.",
                        HttpStatus.BAD_REQUEST);
            }
            Movement m = movementRepository.findByIdAndEmpresaId(movementId, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Movimentacao " + movementId + " nao encontrada."));
            pernas.add(m);
            soma = soma.add(m.getValor() == null ? BigDecimal.ZERO : m.getValor().abs());
        }

        BigDecimal diferenca = alvo.subtract(soma).abs();
        if (diferenca.compareTo(new BigDecimal("0.01")) > 0) {
            throw new BusinessException(
                    "A soma das movimentacoes selecionadas (" + soma + ") nao confere com o valor do "
                    + "extrato (" + alvo + "). Diferenca de " + diferenca + ".",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Regrava as pernas do agrupamento (idempotente).
        matchRepository.deleteByReconciliationId(reconciliation.getId());
        for (Movement m : pernas) {
            ReconciliationMatch perna = new ReconciliationMatch();
            perna.setEmpresaId(empresaId);
            perna.setReconciliationId(reconciliation.getId());
            perna.setMovementId(m.getId());
            perna.setValor(m.getValor());
            matchRepository.save(perna);
        }

        // Sinaliza que ha correspondencia (a 1a perna) e registra a camada de agrupamento.
        reconciliation.setMatchedMovementId(pernas.get(0).getId());
        reconciliation.setCamada("AGRUPAMENTO");
        reconciliation.setScore(BigDecimal.valueOf(100));
        reconciliation.setMotivo("Agrupamento N:1 de " + pernas.size()
                + " movimentacoes (valor conferido)");
        reconciliationRepository.save(reconciliation);
        return toResponse(reconciliation);
    }

    /** Resumo do lote: por status, quantidade e soma dos valores (conciliado x pendente...). */
    @Transactional(readOnly = true)
    public ReconciliationSummary summary(UUID clienteId, LocalDate competencia) {
        List<Object[]> rows = reconciliationRepository.summarize(
                SecurityUtils.currentEmpresaId(), clienteId, competencia);
        List<SummaryLine> porStatus = new ArrayList<>(rows.size());
        long total = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        for (Object[] row : rows) {
            ReconciliationStatus status = (ReconciliationStatus) row[0];
            long quantidade = ((Number) row[1]).longValue();
            BigDecimal valor = row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString());
            porStatus.add(new SummaryLine(status, quantidade, valor));
            total += quantidade;
            valorTotal = valorTotal.add(valor);
        }
        return new ReconciliationSummary(total, valorTotal, porStatus);
    }

    // EB (spec 7/12): nao permite concluir conciliacao se o cliente nao tem plano ativo.
    private void requirePlanoDeContas(Reconciliation reconciliation) {
        UUID clienteId = reconciliation.getClienteId();
        if (clienteId == null) {
            return; // conciliacoes legadas sem cliente nao sao bloqueadas
        }
        boolean temPlano = chartOfAccountRepository.existsPlanoForCliente(
                reconciliation.getEmpresaId(), clienteId);
        if (!temPlano) {
            throw new BusinessException(
                    "Nao foi identificado um plano de contas ativo para este cliente. "
                    + "Configure ou vincule um plano de contas antes de iniciar a conciliacao.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    // A conciliacao so pode ser confirmada contra uma conta ANALITICA (lancavel) do mesmo
    // escopo. Contas sinteticas (agrupadoras) nao recebem lancamento.
    private void requireContaLancavel(Reconciliation reconciliation, UUID contaSugerida) {
        if (contaSugerida == null) {
            return;
        }
        ChartOfAccount conta = chartOfAccountRepository
                .findByIdAndEmpresaId(contaSugerida, reconciliation.getEmpresaId())
                .orElseThrow(() -> new BusinessException(
                        "A conta escolhida nao pertence ao plano de contas desta empresa.",
                        HttpStatus.BAD_REQUEST));
        if (Boolean.FALSE.equals(conta.getAnalitica())) {
            throw new BusinessException(
                    "A conta '" + conta.getCodigo() + " - " + conta.getNome() + "' e sintetica "
                    + "(agrupadora) e nao pode receber lancamento. Escolha uma conta analitica.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Reconciliation findPending(UUID id) {
        return reconciliationRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conciliacao nao encontrada."));
    }

    private void updateMovementStatus(UUID movementId, MovementStatus status) {
        movementRepository.findById(movementId).ifPresent(movement -> {
            movement.setStatus(status);
            movementRepository.save(movement);
        });
    }

    private ReconciliationResponse toResponse(Reconciliation r) {
        // Caminho de item unico (confirm/reject/groupMatch): reutiliza o mesmo builder.
        return buildResponses(List.of(r)).get(0);
    }

    /**
     * Monta as respostas de uma pagina inteira com <b>batch-loading</b> (sem N+1): carrega de
     * uma vez as movimentacoes (principal, correspondencia e pernas de agrupamento), a ultima
     * sugestao de cada item e as contas contabeis, e depois mapeia em memoria.
     */
    private List<ReconciliationResponse> buildResponses(List<Reconciliation> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        // 1) Pernas de agrupamento (N:1) por conciliacao.
        List<UUID> reconIds = items.stream().map(Reconciliation::getId).toList();
        Map<UUID, List<ReconciliationMatch>> legsByRecon = matchRepository.findByReconciliationIdIn(reconIds)
                .stream().collect(Collectors.groupingBy(ReconciliationMatch::getReconciliationId));

        // 2) Todas as movimentacoes referenciadas: principal + correspondencia + pernas.
        Set<UUID> movIds = new HashSet<>();
        for (Reconciliation r : items) {
            movIds.add(r.getMovementId());
            if (r.getMatchedMovementId() != null) {
                movIds.add(r.getMatchedMovementId());
            }
        }
        legsByRecon.values().forEach(legs -> legs.forEach(l -> movIds.add(l.getMovementId())));
        Map<UUID, Movement> movs = movementRepository.findAllById(movIds).stream()
                .collect(Collectors.toMap(Movement::getId, m -> m));

        // 3) Ultima sugestao por movimentacao principal (a lista vem ordenada desc).
        List<UUID> principalIds = items.stream().map(Reconciliation::getMovementId).toList();
        Map<UUID, AiSuggestion> ultimaSugestao = new HashMap<>();
        for (AiSuggestion s : aiSuggestionRepository.findByMovementIdInOrderByCreatedAtDesc(principalIds)) {
            ultimaSugestao.putIfAbsent(s.getMovementId(), s);
        }

        // 4) Contas a resolver (da conta ja escolhida no movimento ou da sugestao).
        Set<UUID> contaIds = new HashSet<>();
        for (Reconciliation r : items) {
            UUID contaId = contaIdFor(r, movs, ultimaSugestao);
            if (contaId != null) {
                contaIds.add(contaId);
            }
        }
        Map<UUID, ChartOfAccount> contas = contaIds.isEmpty() ? Map.of()
                : chartOfAccountRepository.findAllById(contaIds).stream()
                        .collect(Collectors.toMap(ChartOfAccount::getId, c -> c));

        // 5) Mapeia em memoria.
        List<ReconciliationResponse> result = new ArrayList<>(items.size());
        for (Reconciliation r : items) {
            Movement movement = movs.get(r.getMovementId());
            MovementView movimento = movement == null ? null : toMovementView(movement);
            MovementView correspondencia = r.getMatchedMovementId() == null ? null
                    : Optional.ofNullable(movs.get(r.getMatchedMovementId()))
                            .map(this::toMovementView).orElse(null);
            SugestaoView sugestao = toSugestaoView(movement, ultimaSugestao.get(r.getMovementId()), contas);
            List<ReconciliationMatch> legs = legsByRecon.getOrDefault(r.getId(), List.of());
            List<MovementView> agrupamento = legs.isEmpty() ? null : legs.stream()
                    .map(l -> movs.get(l.getMovementId()))
                    .filter(Objects::nonNull)
                    .map(this::toMovementView)
                    .toList();
            result.add(new ReconciliationResponse(r.getId(), r.getClienteId(), r.getCompetencia(),
                    r.getMovementId(), r.getMatchedMovementId(),
                    r.getStatus(), r.getCamada(), r.getScore(), r.getMotivo(),
                    movimento, correspondencia, sugestao, agrupamento));
        }
        return result;
    }

    private UUID contaIdFor(Reconciliation r, Map<UUID, Movement> movs, Map<UUID, AiSuggestion> sugestoes) {
        Movement m = movs.get(r.getMovementId());
        UUID contaId = m != null ? m.getCategoriaSugerida() : null;
        if (contaId == null) {
            AiSuggestion s = sugestoes.get(r.getMovementId());
            contaId = s != null ? s.getContaSugerida() : null;
        }
        return contaId;
    }

    private MovementView toMovementView(Movement m) {
        return new MovementView(m.getId(), m.getData(), m.getValor(), m.getDescricao(),
                m.getDocumento(), m.getBanco(), m.getTipo(), m.getStatus());
    }

    // Conta a sugerir no item: a ja escolhida no movimento tem prioridade; senao, a ultima
    // sugestao (IA/heuristica). Usa os mapas ja carregados (sem consultar o banco de novo).
    private SugestaoView toSugestaoView(Movement movement, AiSuggestion suggestion,
                                        Map<UUID, ChartOfAccount> contas) {
        UUID contaId = movement != null ? movement.getCategoriaSugerida() : null;
        BigDecimal confianca = suggestion != null ? suggestion.getConfianca() : null;
        String origem = suggestion != null ? suggestion.getOrigem() : null;
        if (contaId == null && suggestion != null) {
            contaId = suggestion.getContaSugerida();
        }
        if (contaId == null) {
            return null;
        }
        ChartOfAccount c = contas.get(contaId);
        return c != null
                ? new SugestaoView(c.getId(), c.getCodigo(), c.getNome(), confianca, origem)
                : new SugestaoView(contaId, null, null, confianca, origem);
    }
}
