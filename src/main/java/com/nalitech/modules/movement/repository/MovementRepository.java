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

    List<Movement> findByUploadId(UUID uploadId);

    List<Movement> findByEmpresaIdAndStatus(UUID empresaId, MovementStatus status);

    List<Movement> findByEmpresaIdAndDataAndValor(UUID empresaId, LocalDate data, BigDecimal valor);

    List<Movement> findByEmpresaIdAndValor(UUID empresaId, BigDecimal valor);

    List<Movement> findByEmpresaIdAndDataBetweenAndStatusIn(UUID empresaId, LocalDate inicio,
                                                           LocalDate fim, List<MovementStatus> statuses);

    long countByEmpresaIdAndStatus(UUID empresaId, MovementStatus status);

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
