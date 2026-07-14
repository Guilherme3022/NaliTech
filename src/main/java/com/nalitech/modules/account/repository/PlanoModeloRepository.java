package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.PlanoModelo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoModeloRepository extends JpaRepository<PlanoModelo, UUID> {

    List<PlanoModelo> findByEmpresaIdOrderByNomeAsc(UUID empresaId);

    Optional<PlanoModelo> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
