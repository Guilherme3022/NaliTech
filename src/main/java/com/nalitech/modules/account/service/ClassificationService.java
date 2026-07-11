package com.nalitech.modules.account.service;

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

    public ClassificationService(MovementRepository movementRepository,
                                 LearningService learningService) {
        this.movementRepository = movementRepository;
        this.learningService = learningService;
    }

    public void classify(UUID movementId, UUID contaId) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Movement movement = movementRepository.findByIdAndEmpresaId(movementId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao nao encontrada."));

        movement.setCategoriaSugerida(contaId);
        movement.setStatus(MovementStatus.CLASSIFICADO);
        movementRepository.save(movement);

        learningService.recordDecision(empresaId, movement.getDescricao(), contaId);
    }
}
