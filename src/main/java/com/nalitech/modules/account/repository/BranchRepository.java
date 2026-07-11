package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.Branch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findByEmpresaId(UUID empresaId);

    Optional<Branch> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
