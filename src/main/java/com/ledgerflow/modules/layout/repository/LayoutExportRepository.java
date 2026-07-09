package com.ledgerflow.modules.layout.repository;

import com.ledgerflow.modules.layout.entity.LayoutExport;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LayoutExportRepository extends JpaRepository<LayoutExport, UUID> {

    Page<LayoutExport> findByEmpresaId(UUID empresaId, Pageable pageable);
}
