package com.nalitech.config;

import com.nalitech.modules.user.entity.RoleName;
import com.nalitech.modules.user.entity.User;
import com.nalitech.modules.user.repository.RoleRepository;
import com.nalitech.modules.user.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    public static final UUID DEFAULT_EMPRESA_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${DEFAULT_ADMIN_EMAIL:admin@nalitech.local}") String adminEmail,
                           @Value("${DEFAULT_ADMIN_PASSWORD:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        var adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User admin = new User();
        admin.setEmpresaId(DEFAULT_EMPRESA_ID);
        admin.setName("Administrador");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
        log.info("Usuario ADMIN inicial criado: {}", adminEmail);
    }
}
