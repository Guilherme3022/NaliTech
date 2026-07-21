package com.nalitech.modules.reconciliation.repository;

import com.nalitech.modules.reconciliation.entity.CounterpartAlias;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounterpartAliasRepository extends JpaRepository<CounterpartAlias, UUID> {

    @Query("""
            select a from CounterpartAlias a
            where a.empresaId = :empresaId
              and ((:clienteId is null and a.clienteId is null) or a.clienteId = :clienteId)
              and a.nomeA = :nomeA and a.nomeB = :nomeB
            """)
    Optional<CounterpartAlias> findScoped(@Param("empresaId") UUID empresaId,
                                          @Param("clienteId") UUID clienteId,
                                          @Param("nomeA") String nomeA,
                                          @Param("nomeB") String nomeB);
}
