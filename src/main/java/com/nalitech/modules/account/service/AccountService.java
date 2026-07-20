package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.AccountDtos.AccountRuleRequest;
import com.nalitech.modules.account.dto.AccountDtos.AccountRuleResponse;
import com.nalitech.modules.account.dto.AccountDtos.ChartAccountRequest;
import com.nalitech.modules.account.dto.AccountDtos.ChartAccountResponse;
import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.entity.ChartAccountKind;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.AccountRuleRepository;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final ChartOfAccountRepository chartRepository;
    private final AccountRuleRepository ruleRepository;

    public AccountService(ChartOfAccountRepository chartRepository,
                          AccountRuleRepository ruleRepository) {
        this.chartRepository = chartRepository;
        this.ruleRepository = ruleRepository;
    }

    public ChartAccountResponse createAccount(ChartAccountRequest request) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        // Codigo unico por cliente (ou por empresa quando for conta compartilhada).
        boolean duplicado = request.clienteId() == null
                ? chartRepository.existsByEmpresaIdAndCodigoAndClienteIdIsNull(empresaId, request.codigo())
                : chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(
                        empresaId, request.clienteId(), request.codigo());
        if (duplicado) {
            throw new BusinessException("Ja existe conta com este codigo.", HttpStatus.CONFLICT);
        }
        ChartOfAccount account = new ChartOfAccount();
        account.setEmpresaId(empresaId);
        apply(account, request);
        return toResponse(chartRepository.save(account));
    }

    @Transactional(readOnly = true)
    public Page<ChartAccountResponse> listAccounts(Pageable pageable) {
        return chartRepository.findByEmpresaId(SecurityUtils.currentEmpresaId(), pageable)
                .map(this::toResponse);
    }

    /**
     * Contas lancaveis (analiticas) de um cliente, para o seletor de conta da conciliacao/
     * classificacao. Exclui contas sinteticas (agrupadoras), que nao recebem lancamento.
     */
    @Transactional(readOnly = true)
    public List<ChartAccountResponse> listLancaveis(UUID clienteId) {
        return chartRepository
                .findLancaveisForCliente(SecurityUtils.currentEmpresaId(), clienteId)
                .stream().map(this::toResponse).toList();
    }

    public ChartAccountResponse updateAccount(UUID id, ChartAccountRequest request) {
        ChartOfAccount account = chartRepository
                .findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada."));
        apply(account, request);
        return toResponse(chartRepository.save(account));
    }

    public void deleteAccount(UUID id) {
        ChartOfAccount account = chartRepository
                .findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada."));
        chartRepository.delete(account);
    }

    public AccountRuleResponse createRule(AccountRuleRequest request) {
        AccountRule rule = new AccountRule();
        rule.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<AccountRuleResponse> listRules() {
        return ruleRepository.findByEmpresaId(SecurityUtils.currentEmpresaId())
                .stream().map(this::toResponse).toList();
    }

    public AccountRuleResponse updateRule(UUID id, AccountRuleRequest request) {
        AccountRule rule = ruleRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Regra nao encontrada."));
        apply(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    public void deleteRule(UUID id) {
        AccountRule rule = ruleRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Regra nao encontrada."));
        ruleRepository.delete(rule);
    }

    private void apply(ChartOfAccount account, ChartAccountRequest request) {
        account.setCodigo(request.codigo());
        // Sem valor explicito, classificacao/original espelham o codigo (conta de codigo unico).
        account.setCodigoClassificacao(
                blankTo(request.codigoClassificacao(), request.codigo()));
        account.setCodigoOriginal(
                blankTo(request.codigoOriginal(), request.codigo()));
        account.setNome(request.nome());
        account.setTipo(request.tipo());
        account.setAnalitica(ChartAccountKind.resolveAnalitica(request.analitica(), request.tipo()));
        account.setNaturezaSaldo(request.naturezaSaldo());
        account.setCategoryId(request.categoryId());
        account.setParentId(request.parentId());
        account.setClienteId(request.clienteId());
    }

    private void apply(AccountRule rule, AccountRuleRequest request) {
        rule.setNome(request.nome());
        rule.setDescricaoContains(request.descricaoContains());
        rule.setValorOperador(request.valorOperador());
        rule.setValorRef(request.valorRef());
        rule.setContaId(request.contaId());
        rule.setMarcarRevisao(request.marcarRevisao());
        rule.setPrioridade(request.prioridade());
        rule.setAtivo(request.ativo());
        rule.setClienteId(request.clienteId());
        rule.setCentroCustoId(request.centroCustoId());
        rule.setFilialId(request.filialId());
        rule.setTipoMovimento(request.tipoMovimento());
        rule.setBancoContains(request.bancoContains());
        rule.setDocumentoContains(request.documentoContains());
    }

    private static String blankTo(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private ChartAccountResponse toResponse(ChartOfAccount a) {
        return new ChartAccountResponse(a.getId(), a.getCodigo(),
                a.getCodigoClassificacao(), a.getCodigoOriginal(),
                a.getNome(), a.getTipo(),
                a.getAnalitica(), a.getNaturezaSaldo(),
                a.getCategoryId(), a.getParentId(), a.getClienteId());
    }

    private AccountRuleResponse toResponse(AccountRule r) {
        return new AccountRuleResponse(r.getId(), r.getNome(), r.getDescricaoContains(),
                r.getValorOperador(), r.getValorRef(), r.getContaId(), r.isMarcarRevisao(),
                r.getPrioridade(), r.isAtivo(), r.getClienteId(), r.getCentroCustoId(),
                r.getFilialId(), r.getTipoMovimento(), r.getBancoContains(), r.getDocumentoContains());
    }
}
