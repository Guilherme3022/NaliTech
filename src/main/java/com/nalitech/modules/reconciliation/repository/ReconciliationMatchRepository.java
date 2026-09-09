package com.nalitech.modules.reconciliation.repository;

import com.nalitech.modules.reconciliation.entity.ReconciliationMatch;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationMatchRepository extends JpaRepository<ReconciliationMatch, UUID> {

    List<ReconciliationMatch> findByReconciliationId(UUID reconciliationId);

    // Batch (evita N+1): pernas de varias conciliacoes de uma vez.
    List<ReconciliationMatch> findByReconciliationIdIn(Collection<UUID> reconciliationIds);

    void deleteByReconciliationId(UUID reconciliationId);

    // Limpeza em cascata quando movimentacoes sao removidas (upload apagado, etc.).
    void deleteByMovementIdIn(Collection<UUID> movementIds);

    void deleteByReconciliationIdIn(Collection<UUID> reconciliationIds);
}
