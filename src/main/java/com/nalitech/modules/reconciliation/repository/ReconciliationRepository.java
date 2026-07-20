package com.nalitech.modules.reconciliation.repository;

import com.nalitech.modules.reconciliation.entity.Reconciliation;
import com.nalitech.modules.reconciliation.entity.ReconciliationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Optional<Reconciliation> findByIdAndEmpresaId(UUID id, UUID empresaId);

    // Evita casar a mesma movimentacao do sistema com dois itens de extrato.
    boolean existsByMatchedMovementId(UUID matchedMovementId);

    // Itens de extrato ainda sem correspondencia (para preencher quando o lado
    // sistema chega depois, independente da ordem de upload).
    List<Reconciliation> findByEmpresaIdAndClienteIdAndStatusAndMatchedMovementIdIsNull(
            UUID empresaId, UUID clienteId, ReconciliationStatus status);

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

    // Resumo do lote: por status, quantidade e soma do valor das movimentacoes (theta-join
    // com Movement, pois movementId e uma coluna simples, sem associacao JPA). Filtros
    // opcionais por cliente/competencia (cast evita erro do Postgres com parametro nulo).
    @Query("""
            select r.status, count(r), coalesce(sum(m.valor), 0)
            from Reconciliation r, Movement m
            where m.id = r.movementId
              and r.empresaId = :empresaId
              and (cast(:clienteId as string) is null or r.clienteId = :clienteId)
              and (cast(:competencia as string) is null or r.competencia = :competencia)
            group by r.status
            """)
    List<Object[]> summarize(@Param("empresaId") UUID empresaId,
                             @Param("clienteId") UUID clienteId,
                             @Param("competencia") LocalDate competencia);
}
