package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findByEmpresaId(UUID empresaId);

    Optional<BankAccount> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<BankAccount> findFirstByEmpresaIdAndPadraoTrue(UUID empresaId);

    // Bancos padrao aplicaveis ao cliente: o especifico do cliente tem prioridade
    // sobre o compartilhado (cliente_id null).
    @Query("select b from BankAccount b where b.empresaId = :empresaId and b.padrao = true "
            + "and (b.clienteId = :clienteId or b.clienteId is null) "
            + "order by case when b.clienteId is not null then 0 else 1 end")
    List<BankAccount> findDefaultsApplicable(@Param("empresaId") UUID empresaId,
                                            @Param("clienteId") UUID clienteId);
}
