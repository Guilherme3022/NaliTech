package com.ledgerflow.security;

import com.ledgerflow.shared.exception.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public static AuthenticatedUser requireUser() {
        return currentUser().orElseThrow(() ->
                new BusinessException("Usuario nao autenticado.", HttpStatus.UNAUTHORIZED));
    }

    public static UUID currentEmpresaId() {
        return requireUser().empresaId();
    }
}
