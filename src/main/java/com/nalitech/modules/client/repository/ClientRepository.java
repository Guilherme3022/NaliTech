package com.nalitech.modules.client.repository;

import com.nalitech.modules.client.entity.Client;
import com.nalitech.modules.client.entity.ClientStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndEmpresaId(UUID id, UUID empresaId);

    long countByEmpresaIdAndStatus(UUID empresaId, ClientStatus status);

    List<Client> findByEmpresaIdAndStatus(UUID empresaId, ClientStatus status);

    @Query("""
            select c from Client c
            where c.empresaId = :empresaId
              and (:search is null
                   or lower(c.nome) like lower(concat('%', :search, '%'))
                   or c.cnpjCpf like concat('%', :search, '%'))
            """)
    Page<Client> search(@Param("empresaId") UUID empresaId,
                        @Param("search") String search,
                        Pageable pageable);
}
