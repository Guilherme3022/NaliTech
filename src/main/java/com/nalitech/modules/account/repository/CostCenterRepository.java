package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.CostCenter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, UUID> {

    List<CostCenter> findByEmpresaId(UUID empresaId);

    Optional<CostCenter> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
