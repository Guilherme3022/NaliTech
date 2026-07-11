package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.LoanContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanContractRepository extends JpaRepository<LoanContract, UUID> {

    List<LoanContract> findByEmpresaId(UUID empresaId);

    Optional<LoanContract> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
