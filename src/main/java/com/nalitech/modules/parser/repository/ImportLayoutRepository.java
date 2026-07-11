package com.nalitech.modules.parser.repository;

import com.nalitech.modules.parser.entity.ImportLayout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportLayoutRepository extends JpaRepository<ImportLayout, UUID> {

    List<ImportLayout> findByEmpresaId(UUID empresaId);

    Optional<ImportLayout> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
