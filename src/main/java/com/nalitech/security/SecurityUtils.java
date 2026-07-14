package com.nalitech.security;

import com.nalitech.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    /**
     * Empresa efetiva da requisicao.
     *
     * <p>O ADMIN geral fica acima das empresas e pode operar dentro de uma
     * empresa selecionada, informada no header {@code X-Empresa-Id}. Para os
     * demais perfis o header e ignorado (ficam presos a propria empresa), o que
     * evita que um usuario comum acesse dados de outra empresa.
     */
    public static UUID currentEmpresaId() {
        AuthenticatedUser user = requireUser();
        if (user.roles().contains("ADMIN")) {
            UUID selecionada = selectedEmpresaHeader();
            if (selecionada != null) {
                return selecionada;
            }
        }
        return user.empresaId();
    }

    /**
     * Empresa efetiva obrigatoria (para operacoes que gravam dados).
     * Lanca erro quando o ADMIN geral nao selecionou nenhuma empresa.
     */
    public static UUID requireEmpresaId() {
        UUID empresaId = currentEmpresaId();
        if (empresaId == null) {
            throw new BusinessException(
                    "Selecione uma empresa para realizar esta acao.", HttpStatus.BAD_REQUEST);
        }
        return empresaId;
    }

    private static UUID selectedEmpresaHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String header = request.getHeader("X-Empresa-Id");
            if (header != null && !header.isBlank()) {
                try {
                    return UUID.fromString(header.trim());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
