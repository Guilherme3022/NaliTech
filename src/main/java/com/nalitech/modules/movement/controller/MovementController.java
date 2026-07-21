package com.nalitech.modules.movement.controller;

import com.nalitech.modules.movement.dto.MovementDtos.MovementResponse;
import com.nalitech.modules.movement.dto.MovementDtos.UpdateMovementRequest;
import com.nalitech.modules.movement.service.MovementService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movements")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public Page<MovementResponse> list(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String competencia,
            Pageable pageable) {
        return movementService.list(clienteId, origem, parseCompetencia(competencia), pageable);
    }

    @PutMapping("/{id}")
    public MovementResponse update(@PathVariable UUID id, @RequestBody UpdateMovementRequest request) {
        return movementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        movementService.delete(id);
    }

    private LocalDate parseCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return null;
        }
        return LocalDate.parse(competencia.trim() + "-01");
    }
}
