package com.ledgerflow.modules.user.repository;

import com.ledgerflow.modules.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByEmpresaId(UUID empresaId, Pageable pageable);

    Optional<User> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
