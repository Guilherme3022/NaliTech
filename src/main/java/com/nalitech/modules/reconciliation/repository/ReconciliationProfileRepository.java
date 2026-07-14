package com.nalitech.modules.reconciliation.repository;

import com.nalitech.modules.reconciliation.entity.ReconciliationProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliationProfileRepository extends JpaRepository<ReconciliationProfile, UUID> {

    Optional<ReconciliationProfile> findByIdAndEmpresaId(UUID id, UUID empresaId);

    // cast(:clienteId as string) evita o erro do PostgreSQL com filtro opcional nulo.
    @Query("""
            select p from ReconciliationProfile p
            where p.empresaId = :empresaId
              and (cast(:clienteId as string) is null or p.clienteId = :clienteId)
            order by p.nome asc
            """)
    List<ReconciliationProfile> search(@Param("empresaId") UUID empresaId,
                                       @Param("clienteId") UUID clienteId);
}
