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

    // cast(:param as string) da tipo ao parametro nulo, evitando o erro do PostgreSQL
    // "could not determine data type of parameter" no filtro opcional (is null).
    @Query("""
            select a from AuditLog a
            where a.empresaId = :empresaId
              and (cast(:usuarioId as string) is null or a.usuarioId = :usuarioId)
              and (cast(:entidade as string) is null or a.entidade = :entidade)
              and (cast(:inicio as string) is null or a.timestamp >= :inicio)
              and (cast(:fim as string) is null or a.timestamp <= :fim)
            order by a.timestamp desc
            """)
    Page<AuditLog> search(@Param("empresaId") UUID empresaId,
                         @Param("usuarioId") UUID usuarioId,
                         @Param("entidade") String entidade,
                         @Param("inicio") OffsetDateTime inicio,
                         @Param("fim") OffsetDateTime fim,
                         Pageable pageable);
}
