package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.AccountDtos.ClassifyRequest;
import com.nalitech.modules.account.dto.AccountDtos.SuggestionResponse;
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
}
