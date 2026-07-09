package com.ledgerflow.modules.account.service;

import com.ledgerflow.modules.movement.entity.Movement;
import com.ledgerflow.modules.movement.entity.MovementStatus;
import com.ledgerflow.modules.movement.repository.MovementRepository;
import com.ledgerflow.security.SecurityUtils;
import com.ledgerflow.shared.exception.ResourceNotFoundException;
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
