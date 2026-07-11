package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.LearningHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningHistoryRepository extends JpaRepository<LearningHistory, UUID> {

    Optional<LearningHistory> findByEmpresaIdAndDescricaoPadrao(UUID empresaId, String descricaoPadrao);

    List<LearningHistory> findByEmpresaId(UUID empresaId);

    // Aprendizado escopado por cliente (Increment 3, Q3). Trata cliente_id null.
    @Query("select h from LearningHistory h where h.empresaId = :empresaId "
            + "and h.descricaoPadrao = :padrao "
            + "and ((:clienteId is null and h.clienteId is null) or h.clienteId = :clienteId)")
    Optional<LearningHistory> findScoped(@Param("empresaId") UUID empresaId,
                                        @Param("clienteId") UUID clienteId,
                                        @Param("padrao") String padrao);

    @Query("select h from LearningHistory h where h.empresaId = :empresaId "
            + "and ((:clienteId is null and h.clienteId is null) or h.clienteId = :clienteId)")
    List<LearningHistory> findByScope(@Param("empresaId") UUID empresaId,
                                      @Param("clienteId") UUID clienteId);
}
