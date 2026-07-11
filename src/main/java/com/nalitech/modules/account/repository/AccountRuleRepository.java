package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.AccountRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRuleRepository extends JpaRepository<AccountRule, UUID> {

    List<AccountRule> findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(UUID empresaId);

    List<AccountRule> findByEmpresaId(UUID empresaId);

    Optional<AccountRule> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
