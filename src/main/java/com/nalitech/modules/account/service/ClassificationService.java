package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.LoanContract;
import com.nalitech.modules.account.repository.LoanContractRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClassificationService {

    private final MovementRepository movementRepository;
    private final LearningService learningService;
    private final DoubleEntryService doubleEntryService;
    private final RuleEngineService ruleEngineService;
    private final LoanContractRepository loanContractRepository;

    public ClassificationService(MovementRepository movementRepository,
                                 LearningService learningService,
                                 DoubleEntryService doubleEntryService,
                                 RuleEngineService ruleEngineService,
                                 LoanContractRepository loanContractRepository) {
        this.movementRepository = movementRepository;
        this.learningService = learningService;
        this.doubleEntryService = doubleEntryService;
        this.ruleEngineService = ruleEngineService;
        this.loanContractRepository = loanContractRepository;
    }

    /** Classifica pela conta de contrapartida (De/Para) e monta a partida dobrada. */
    public void classify(UUID movementId, UUID contaId) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Movement movement = movementRepository.findByIdAndEmpresaId(movementId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));

        movement.setCategoriaSugerida(contaId);
        doubleEntryService.applyCounterpart(movement, contaId);
        // Centro de custo e filial: aplicados automaticamente se a regra que casa os definir.
        ruleEngineService.firstMatching(movement).ifPresent(rule -> {
            if (rule.getCentroCustoId() != null) {
                movement.setCentroCustoId(rule.getCentroCustoId());
            }
            if (rule.getFilialId() != null) {
                movement.setFilialId(rule.getFilialId());
            }
        });
        movement.setStatus(MovementStatus.CLASSIFICADO);
        movementRepository.save(movement);

        learningService.recordDecision(empresaId, movement.getClienteId(), movement.getDescricao(), contaId);
    }

    /** Ajuste manual do lancamento: define debito e credito diretamente. */
    public void setEntry(UUID movementId, UUID contaDebitoId, UUID contaCreditoId) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Movement movement = movementRepository.findByIdAndEmpresaId(movementId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));

        movement.setContaDebitoId(contaDebitoId);
        movement.setContaCreditoId(contaCreditoId);
        movement.setStatus(MovementStatus.CLASSIFICADO);
        movementRepository.save(movement);
    }

    /** Atribuicao manual de centro de custo a um lancamento. */
    public void setCostCenter(UUID movementId, UUID centroCustoId) {
        Movement movement = movementRepository
                .findByIdAndEmpresaId(movementId, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));
        movement.setCentroCustoId(centroCustoId);
        movementRepository.save(movement);
    }

    /** Atribuicao manual de filial a um lancamento. */
    public void setBranch(UUID movementId, UUID filialId) {
        Movement movement = movementRepository
                .findByIdAndEmpresaId(movementId, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));
        movement.setFilialId(filialId);
        movementRepository.save(movement);
    }

    /**
     * Vincula o lancamento a um contrato de financiamento. Se o contrato tiver conta
     * de principal, ja classifica o lancamento nela (o contador ajusta se for juros/encargo).
     */
    public void setLoanContract(UUID movementId, UUID loanContractId) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Movement movement = movementRepository.findByIdAndEmpresaId(movementId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));
        movement.setLoanContractId(loanContractId);

        if (loanContractId != null) {
            LoanContract contract = loanContractRepository.findByIdAndEmpresaId(loanContractId, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contrato nao encontrado."));
            if (contract.getContaPrincipalId() != null) {
                movement.setCategoriaSugerida(contract.getContaPrincipalId());
                doubleEntryService.applyCounterpart(movement, contract.getContaPrincipalId());
                movement.setStatus(MovementStatus.CLASSIFICADO);
            }
        }
        movementRepository.save(movement);
    }
}
