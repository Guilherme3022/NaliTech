package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.ChartOfAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {

    Page<ChartOfAccount> findByEmpresaId(UUID empresaId, Pageable pageable);

    List<ChartOfAccount> findByEmpresaId(UUID empresaId);

    Optional<ChartOfAccount> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);
}
