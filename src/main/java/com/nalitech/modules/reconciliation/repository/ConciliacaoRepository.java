package com.nalitech.modules.reconciliation.repository;

import com.nalitech.modules.reconciliation.entity.Conciliacao;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConciliacaoRepository extends JpaRepository<Conciliacao, UUID> {

    Optional<Conciliacao> findByIdAndEmpresaId(UUID id, UUID empresaId);

    // Lista para os cards. cast(:param as string) evita o erro do PostgreSQL
    // "could not determine data type" quando o filtro opcional vem nulo.
    @Query("""
            select c from Conciliacao c
            where c.empresaId = :empresaId
              and (cast(:clienteId as string) is null or c.clienteId = :clienteId)
              and (cast(:competencia as string) is null or c.competencia = :competencia)
            order by c.competencia desc, c.createdAt desc
            """)
    List<Conciliacao> search(@Param("empresaId") UUID empresaId,
                             @Param("clienteId") UUID clienteId,
                             @Param("competencia") LocalDate competencia);
}
