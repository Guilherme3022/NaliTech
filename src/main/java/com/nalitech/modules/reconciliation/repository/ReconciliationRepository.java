package com.nalitech.modules.reconciliation.repository;

import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Optional<Reconciliation> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Page<Reconciliation> findByEmpresaIdAndStatus(UUID empresaId, ReconciliationStatus status,
                                                  Pageable pageable);

    // EA: filtro opcional por cliente e competencia. cast(... as string) evita o
    // erro do PostgreSQL "could not determine data type" com parametro nulo.
    @Query("""
            select r from Reconciliation r
            where r.empresaId = :empresaId
              and r.status = :status
              and (cast(:clienteId as string) is null or r.clienteId = :clienteId)
              and (cast(:competencia as string) is null or r.competencia = :competencia)
            """)
    Page<Reconciliation> search(@Param("empresaId") UUID empresaId,
                                @Param("status") ReconciliationStatus status,
                                @Param("clienteId") UUID clienteId,
                                @Param("competencia") LocalDate competencia,
                                Pageable pageable);

    long countByEmpresaIdAndStatus(UUID empresaId, ReconciliationStatus status);
}
