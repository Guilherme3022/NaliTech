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

    // Limpeza em cascata quando um upload (e suas movimentacoes) e removido.
    void deleteByMovementIdIn(java.util.Collection<UUID> movementIds);

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

    // Reprocessamento: pendencias sem correspondencia (camada MANUAL) para re-rodar
    // o matching apos novas regras / com a IA ligada. Filtros opcionais.
    @Query("""
            select r from Reconciliation r
            where r.empresaId = :empresaId
              and r.status = com.nalitech.modules.reconciliation.entity.ReconciliationStatus.PENDENTE
              and r.camada = 'MANUAL'
              and (cast(:clienteId as string) is null or r.clienteId = :clienteId)
              and (cast(:competencia as string) is null or r.competencia = :competencia)
            """)
    java.util.List<Reconciliation> findManualPending(@Param("empresaId") UUID empresaId,
                                                     @Param("clienteId") UUID clienteId,
                                                     @Param("competencia") LocalDate competencia);

    // Sweep de IA: pendencias MANUAL que o LLM ainda NAO avaliou (ia_tentada=false).
    // Evita re-chamar a IA para itens ja analisados (memoria/cache).
    @Query("""
            select r from Reconciliation r
            where r.empresaId = :empresaId
              and r.status = com.nalitech.modules.reconciliation.entity.ReconciliationStatus.PENDENTE
              and r.camada = 'MANUAL'
              and r.iaTentada = false
              and (cast(:clienteId as string) is null or r.clienteId = :clienteId)
              and (cast(:competencia as string) is null or r.competencia = :competencia)
            """)
    java.util.List<Reconciliation> findManualPendingNotAiTried(@Param("empresaId") UUID empresaId,
                                                              @Param("clienteId") UUID clienteId,
                                                              @Param("competencia") LocalDate competencia);
}
