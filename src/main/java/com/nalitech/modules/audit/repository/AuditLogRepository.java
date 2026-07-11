package com.nalitech.modules.audit.repository;

import com.nalitech.modules.audit.entity.AuditLog;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a from AuditLog a
            where a.empresaId = :empresaId
              and (:usuarioId is null or a.usuarioId = :usuarioId)
              and (:entidade is null or a.entidade = :entidade)
              and (:inicio is null or a.timestamp >= :inicio)
              and (:fim is null or a.timestamp <= :fim)
            order by a.timestamp desc
            """)
    Page<AuditLog> search(@Param("empresaId") UUID empresaId,
                         @Param("usuarioId") UUID usuarioId,
                         @Param("entidade") String entidade,
                         @Param("inicio") OffsetDateTime inicio,
                         @Param("fim") OffsetDateTime fim,
                         Pageable pageable);
}
