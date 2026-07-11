package com.nalitech.modules.apikey.repository;

import com.nalitech.modules.apikey.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByChaveHashAndAtivoTrue(String chaveHash);

    List<ApiKey> findByEmpresaId(UUID empresaId);

    Optional<ApiKey> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
