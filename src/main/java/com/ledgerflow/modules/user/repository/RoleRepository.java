package com.ledgerflow.modules.user.repository;

import com.ledgerflow.modules.user.entity.Role;
import com.ledgerflow.modules.user.entity.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
