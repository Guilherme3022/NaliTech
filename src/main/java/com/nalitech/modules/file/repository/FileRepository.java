package com.nalitech.modules.file.repository;

import com.nalitech.modules.file.entity.FileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Optional<FileEntity> findByEmpresaIdAndHashSha256(UUID empresaId, String hashSha256);

    Optional<FileEntity> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
