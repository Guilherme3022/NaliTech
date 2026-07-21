package com.nalitech.modules.movement.repository;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    Optional<Movement> findByIdAndEmpresaId(UUID id, UUID empresaId);

    // Tela de Movimentacoes: lista paginada por cliente/competencia (filtros opcionais).
    // cast(:param as string) evita o erro do PostgreSQL com parametro nulo.
    @Query("""
            select m from Movement m
            where m.empresaId = :empresaId
              and (cast(:clienteId as string) is null or m.clienteId = :clienteId)
              and (cast(:inicio as string) is null or m.data >= :inicio)
              and (cast(:fim as string) is null or m.data <= :fim)
            order by m.data desc
            """)
    org.springframework.data.domain.Page<Movement> search(
            @Param("empresaId") UUID empresaId,
            @Param("clienteId") UUID clienteId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            org.springframework.data.domain.Pageable pageable);

    List<Movement> findByUploadId(UUID uploadId);

    List<Movement> findByEmpresaIdAndStatus(UUID empresaId, MovementStatus status);

    List<Movement> findByEmpresaIdAndDataAndValor(UUID empresaId, LocalDate data, BigDecimal valor);

    List<Movement> findByEmpresaIdAndValor(UUID empresaId, BigDecimal valor);

    // Candidatos livres para casar com uma movimentacao: mesmo cliente, de OUTRO arquivo
    // (uploadId diferente, evita casar linhas do mesmo documento), ainda nao conciliados
    // (NORMALIZADO) e com data dentro de uma janela. O papel do documento (extrato x
    // sistema) NAO e exigido aqui (vira apenas um bonus no score) para casar mesmo quando
    // o usuario nao marcou os papeis. O casamento por valor/nome/data e pontuado em memoria.
    @Query("""
            select m from Movement m
            where m.empresaId = :empresaId
              and m.clienteId = :clienteId
              and m.uploadId <> :excludeUploadId
              and m.status = com.nalitech.modules.movement.entity.MovementStatus.NORMALIZADO
              and m.data between :inicio and :fim
            """)
    List<Movement> findMatchCandidatesInWindow(@Param("empresaId") UUID empresaId,
                                               @Param("clienteId") UUID clienteId,
                                               @Param("excludeUploadId") UUID excludeUploadId,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);

    List<Movement> findByEmpresaIdAndDataBetweenAndStatusIn(UUID empresaId, LocalDate inicio,
                                                           LocalDate fim, List<MovementStatus> statuses);

    long countByEmpresaIdAndStatus(UUID empresaId, MovementStatus status);

    // Export por conciliacao (EF): movimentacoes do cliente na competencia.
    List<Movement> findByEmpresaIdAndClienteIdAndDataBetweenOrderByData(
            UUID empresaId, UUID clienteId, LocalDate inicio, LocalDate fim);

    // Dashboard (Increment 8): aguardando classificacao/parametrizacao.
    long countByEmpresaIdAndStatusAndCategoriaSugeridaIsNull(UUID empresaId, MovementStatus status);

    long countByEmpresaIdAndClienteIdAndStatus(UUID empresaId, UUID clienteId, MovementStatus status);

    long countByEmpresaIdAndClienteIdAndStatusAndCategoriaSugeridaIsNull(
            UUID empresaId, UUID clienteId, MovementStatus status);

    // Movimentacoes prontas para classificar, ainda sem De/Para (conta) definido.
    List<Movement> findByEmpresaIdAndStatusAndCategoriaSugeridaIsNull(UUID empresaId, MovementStatus status);

    // Idem, filtrando por descricao que contem um termo (para aplicar De/Para em lote).
    @Query("select m from Movement m where m.empresaId = :empresaId and m.status = :status "
            + "and m.categoriaSugerida is null and m.descricao is not null "
            + "and lower(m.descricao) like lower(concat('%', :termo, '%'))")
    List<Movement> findPendingByDescricaoContains(@Param("empresaId") UUID empresaId,
                                                  @Param("status") MovementStatus status,
                                                  @Param("termo") String termo);
}
