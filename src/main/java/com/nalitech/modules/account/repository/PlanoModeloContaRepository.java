package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.PlanoModeloConta;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoModeloContaRepository extends JpaRepository<PlanoModeloConta, UUID> {

    List<PlanoModeloConta> findByModeloIdAndEmpresaIdOrderByCodigoAsc(UUID modeloId, UUID empresaId);
}
