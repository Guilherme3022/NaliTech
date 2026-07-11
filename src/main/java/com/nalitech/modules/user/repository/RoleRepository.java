package com.nalitech.modules.user.repository;

import com.nalitech.modules.user.entity.Role;
import com.nalitech.modules.user.entity.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
