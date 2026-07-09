package com.ledgerflow.modules.file.repository;

import com.ledgerflow.modules.file.entity.Upload;
import com.ledgerflow.modules.file.entity.UploadStatus;
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

    @Query("""
            select u from Upload u
            where u.empresaId = :empresaId
              and (:clienteId is null or u.clienteId = :clienteId)
              and (:inicio is null or u.createdAt >= :inicio)
              and (:fim is null or u.createdAt <= :fim)
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
