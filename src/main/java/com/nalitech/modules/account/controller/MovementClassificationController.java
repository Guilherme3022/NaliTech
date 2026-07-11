package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.AccountDtos.BranchAssignRequest;
import com.nalitech.modules.account.dto.AccountDtos.ClassifyRequest;
import com.nalitech.modules.account.dto.AccountDtos.CostCenterAssignRequest;
import com.nalitech.modules.account.dto.AccountDtos.LoanContractAssignRequest;
import com.nalitech.modules.account.dto.AccountDtos.ManualEntryRequest;
import com.nalitech.modules.account.dto.AccountDtos.SuggestionResponse;
import jakarta.validation.Valid;
import com.nalitech.modules.account.entity.AiSuggestion;
import com.nalitech.modules.account.service.ClassificationService;
import com.nalitech.modules.account.service.ClassificationSuggestionService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movements")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class MovementClassificationController {

    private final ClassificationSuggestionService suggestionService;
    private final ClassificationService classificationService;

    public MovementClassificationController(ClassificationSuggestionService suggestionService,
                                            ClassificationService classificationService) {
        this.suggestionService = suggestionService;
        this.classificationService = classificationService;
    }

    @GetMapping("/{id}/suggestions")
    public SuggestionResponse suggestions(@PathVariable UUID id) {
        AiSuggestion suggestion = suggestionService.suggestFor(id);
        return new SuggestionResponse(suggestion.getMovementId(), suggestion.getContaSugerida(),
                suggestion.getConfianca(), suggestion.getOrigem());
    }

    @PostMapping("/{id}/classify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void classify(@PathVariable UUID id, @RequestBody ClassifyRequest request) {
        classificationService.classify(id, request.contaId());
    }

    /** Ajuste manual do lancamento: define debito e credito diretamente (partida dobrada). */
    @PostMapping("/{id}/entry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEntry(@PathVariable UUID id, @Valid @RequestBody ManualEntryRequest request) {
        classificationService.setEntry(id, request.contaDebitoId(), request.contaCreditoId());
    }

    /** Atribuicao manual de centro de custo (Increment 4). */
    @PostMapping("/{id}/cost-center")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setCostCenter(@PathVariable UUID id, @RequestBody CostCenterAssignRequest request) {
        classificationService.setCostCenter(id, request.centroCustoId());
    }

    /** Atribuicao manual de filial (Increment 5). */
    @PostMapping("/{id}/branch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBranch(@PathVariable UUID id, @RequestBody BranchAssignRequest request) {
        classificationService.setBranch(id, request.filialId());
    }

    /** Vincula o lancamento a um contrato de financiamento (Increment 7). */
    @PostMapping("/{id}/loan-contract")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLoanContract(@PathVariable UUID id, @RequestBody LoanContractAssignRequest request) {
        classificationService.setLoanContract(id, request.loanContractId());
    }
}
