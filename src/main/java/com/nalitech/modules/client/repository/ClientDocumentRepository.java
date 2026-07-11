package com.nalitech.modules.client.repository;

import com.nalitech.modules.client.entity.ClientDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientDocumentRepository extends JpaRepository<ClientDocument, UUID> {

    List<ClientDocument> findByClienteIdAndEmpresaId(UUID clienteId, UUID empresaId);
}
