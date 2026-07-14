package com.nalitech.modules.user.service;

import com.nalitech.modules.audit.Audited;
import com.nalitech.modules.user.dto.UserDtos.CreateUserRequest;
import com.nalitech.modules.user.dto.UserDtos.UpdateUserRequest;
import com.nalitech.modules.user.dto.UserDtos.UserResponse;
import com.nalitech.modules.user.entity.Role;
import com.nalitech.modules.user.entity.RoleName;
import com.nalitech.modules.user.entity.User;
import com.nalitech.modules.user.entity.UserStatus;
import com.nalitech.modules.user.mapper.UserMapper;
import com.nalitech.modules.user.repository.RoleRepository;
import com.nalitech.modules.user.repository.UserRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ja existe um usuario com este e-mail.", HttpStatus.CONFLICT);
        }
        // Item 5: apenas o ADMIN geral pode existir sem empresa. Usuario comum
        // (CONTADOR/AUXILIAR/CLIENTE) precisa estar vinculado a uma empresa.
        UUID empresaId = SecurityUtils.currentEmpresaId();
        boolean isAdmin = request.roles().contains(RoleName.ADMIN);
        if (!isAdmin && empresaId == null) {
            throw new BusinessException(
                    "Selecione uma empresa antes de criar um usuario comum.", HttpStatus.BAD_REQUEST);
        }
        User user = new User();
        user.setEmpresaId(empresaId);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setClienteId(request.clienteId());
        user.setRoles(resolveRoles(request.roles()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findByEmpresaId(SecurityUtils.currentEmpresaId(), pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findInCurrentCompany(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrent() {
        UUID id = SecurityUtils.requireUser().id();
        return userMapper.toResponse(findInCurrentCompany(id));
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findInCurrentCompany(id);
        user.setName(request.name());
        user.setRoles(resolveRoles(request.roles()));
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Audited(action = "EXCLUSAO_USUARIO", entity = "USER")
    public void delete(UUID id) {
        User user = findInCurrentCompany(id);

        user.setStatus(UserStatus.INATIVO);
        userRepository.save(user);
    }

    private User findInCurrentCompany(UUID id) {
        return userRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = names.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new BusinessException(
                                "Perfil invalido: " + name, HttpStatus.BAD_REQUEST)))
                .collect(Collectors.toCollection(HashSet::new));
        if (roles.isEmpty()) {
            throw new BusinessException("Informe ao menos um perfil.", HttpStatus.BAD_REQUEST);
        }
        return roles;
    }
}
