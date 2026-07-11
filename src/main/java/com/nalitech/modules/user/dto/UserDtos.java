package com.nalitech.modules.user.dto;

import com.nalitech.modules.auth.validator.StrongPassword;
import com.nalitech.modules.user.entity.RoleName;
import com.nalitech.modules.user.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @StrongPassword String password,
            @NotEmpty Set<RoleName> roles,
            UUID clienteId) {
    }

    public record UpdateUserRequest(
            @NotBlank String name,
            @NotEmpty Set<RoleName> roles,
            UserStatus status) {
    }

    public record UserResponse(
            UUID id,
            String name,
            String email,
            UserStatus status,
            boolean twoFactorEnabled,
            Set<RoleName> roles,
            UUID clienteId) {
    }
}
