package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.ChartOfAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {

    // EB: o cliente tem plano de contas se existir ao menos uma conta especifica
    // dele OU uma conta compartilhada do escritorio (cliente_id nulo).
    @Query("""
            select count(c) > 0 from ChartOfAccount c
            where c.empresaId = :empresaId
              and (c.clienteId = :clienteId or c.clienteId is null)
            """)
    boolean existsPlanoForCliente(@Param("empresaId") UUID empresaId,
                                  @Param("clienteId") UUID clienteId);

    Page<ChartOfAccount> findByEmpresaId(UUID empresaId, Pageable pageable);

    List<ChartOfAccount> findByEmpresaId(UUID empresaId);

    // Contas lancaveis (conciliacao/sugestoes): analiticas ou ainda indefinidas (null).
    // Contas sinteticas (analitica = false) sao agrupadoras e nunca recebem lancamento.
    @Query("""
            select c from ChartOfAccount c
            where c.empresaId = :empresaId
              and (c.analitica is null or c.analitica = true)
            """)
    List<ChartOfAccount> findLancaveisByEmpresa(@Param("empresaId") UUID empresaId);

    // Contas lancaveis (analiticas) visiveis para um cliente: as especificas dele + as
    // compartilhadas do escritorio (cliente_id nulo). Usado no seletor de conta da conciliacao.
    @Query("""
            select c from ChartOfAccount c
            where c.empresaId = :empresaId
              and (c.clienteId = :clienteId or c.clienteId is null)
              and (c.analitica is null or c.analitica = true)
            order by c.codigo asc
            """)
    List<ChartOfAccount> findLancaveisForCliente(@Param("empresaId") UUID empresaId,
                                                 @Param("clienteId") UUID clienteId);

    Optional<ChartOfAccount> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    // Unicidade de codigo por cliente (conta especifica) ou compartilhada (cliente nulo).
    boolean existsByEmpresaIdAndClienteIdAndCodigo(UUID empresaId, UUID clienteId, String codigo);

    boolean existsByEmpresaIdAndCodigoAndClienteIdIsNull(UUID empresaId, String codigo);
}
