package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.AccountRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRuleRepository extends JpaRepository<AccountRule, UUID> {

    List<AccountRule> findByEmpresaIdAndAtivoTrueOrderByPrioridadeDesc(UUID empresaId);

    // Regras aplicaveis ao cliente: especificas dele + compartilhadas (cliente_id null).
    // As especificas do cliente vem primeiro (tem prioridade sobre as compartilhadas).
    @Query("select r from AccountRule r where r.empresaId = :empresaId and r.ativo = true "
            + "and (r.clienteId = :clienteId or r.clienteId is null) "
            + "order by case when r.clienteId is not null then 0 else 1 end, r.prioridade desc")
    List<AccountRule> findApplicable(@Param("empresaId") UUID empresaId,
                                    @Param("clienteId") UUID clienteId);

    List<AccountRule> findByEmpresaId(UUID empresaId);

    Optional<AccountRule> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
