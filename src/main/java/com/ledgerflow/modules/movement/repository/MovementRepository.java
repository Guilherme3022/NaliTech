package com.ledgerflow.modules.movement.repository;

import com.ledgerflow.modules.movement.entity.Movement;
import com.ledgerflow.modules.movement.entity.MovementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    Optional<Movement> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Movement> findByUploadId(UUID uploadId);

    List<Movement> findByEmpresaIdAndStatus(UUID empresaId, MovementStatus status);

    List<Movement> findByEmpresaIdAndDataAndValor(UUID empresaId, LocalDate data, BigDecimal valor);

    List<Movement> findByEmpresaIdAndValor(UUID empresaId, BigDecimal valor);

    List<Movement> findByEmpresaIdAndDataBetweenAndStatusIn(UUID empresaId, LocalDate inicio,
                                                           LocalDate fim, List<MovementStatus> statuses);

    long countByEmpresaIdAndStatus(UUID empresaId, MovementStatus status);
}
