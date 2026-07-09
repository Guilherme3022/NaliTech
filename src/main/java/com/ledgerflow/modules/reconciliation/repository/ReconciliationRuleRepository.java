package com.ledgerflow.modules.reconciliation.repository;

import com.ledgerflow.modules.reconciliation.entity.ReconciliationRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRuleRepository extends JpaRepository<ReconciliationRule, UUID> {

    List<ReconciliationRule> findByEmpresaIdAndAtivoTrue(UUID empresaId);
}
