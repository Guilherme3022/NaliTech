package com.nalitech.modules.file.repository;

import com.nalitech.modules.file.entity.Upload;
import com.nalitech.modules.file.entity.UploadStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

    Optional<Upload> findByIdAndEmpresaId(UUID id, UUID empresaId);

    // cast(:param as string) da tipo ao parametro nulo, evitando o erro do PostgreSQL
    // "could not determine data type of parameter" no filtro opcional (is null).
    @Query("""
            select u from Upload u
            where u.empresaId = :empresaId
              and (cast(:clienteId as string) is null or u.clienteId = :clienteId)
              and (cast(:inicio as string) is null or u.createdAt >= :inicio)
              and (cast(:fim as string) is null or u.createdAt <= :fim)
            """)
    Page<Upload> filter(@Param("empresaId") UUID empresaId,
                       @Param("clienteId") UUID clienteId,
                       @Param("inicio") OffsetDateTime inicio,
                       @Param("fim") OffsetDateTime fim,
                       Pageable pageable);

    long countByEmpresaIdAndStatus(UUID empresaId, UploadStatus status);

    long countByEmpresaIdAndCreatedAtAfter(UUID empresaId, OffsetDateTime after);

    java.util.List<Upload> findTop10ByEmpresaIdOrderByCreatedAtDesc(UUID empresaId);
}
