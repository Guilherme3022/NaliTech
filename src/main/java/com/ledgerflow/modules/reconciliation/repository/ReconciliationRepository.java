package com.ledgerflow.modules.reconciliation.repository;

import com.ledgerflow.modules.reconciliation.entity.Reconciliation;
import com.ledgerflow.modules.reconciliation.entity.ReconciliationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Optional<Reconciliation> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Page<Reconciliation> findByEmpresaIdAndStatus(UUID empresaId, ReconciliationStatus status,
                                                  Pageable pageable);

    long countByEmpresaIdAndStatus(UUID empresaId, ReconciliationStatus status);
}
