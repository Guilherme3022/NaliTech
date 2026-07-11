package com.nalitech.modules.finance.repository;

import com.nalitech.modules.finance.entity.OfficeFee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeFeeRepository extends JpaRepository<OfficeFee, UUID> {

    List<OfficeFee> findByEmpresaId(UUID empresaId);

    Optional<OfficeFee> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
