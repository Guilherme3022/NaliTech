package com.ledgerflow.modules.auth.service;

import com.ledgerflow.config.JwtProperties;
import com.ledgerflow.modules.audit.Audited;
import com.ledgerflow.modules.auth.dto.AuthDtos.LoginResponse;
import com.ledgerflow.modules.auth.entity.RefreshToken;
import com.ledgerflow.modules.auth.repository.RefreshTokenRepository;
import com.ledgerflow.modules.user.entity.Role;
import com.ledgerflow.modules.user.entity.User;
import com.ledgerflow.modules.user.repository.UserRepository;
import com.ledgerflow.security.AuthenticatedUser;
import com.ledgerflow.security.JwtService;
import com.ledgerflow.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Audited(action = "LOGIN", entity = "USER")
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))

                .orElseThrow(() -> new BusinessException("Credenciais invalidas.", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new BusinessException("Usuario inativo.", HttpStatus.FORBIDDEN);
        }
        return issueTokens(user);
    }

    public LoginResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException("Refresh token invalido.", HttpStatus.UNAUTHORIZED));
        if (!stored.isUsable()) {
            throw new BusinessException("Refresh token expirado ou revogado.", HttpStatus.UNAUTHORIZED);
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado.", HttpStatus.UNAUTHORIZED));
        return issueTokens(user);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private LoginResponse issueTokens(User user) {
        AuthenticatedUser principal = toPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = persistRefreshToken(user.getId());
        return new LoginResponse(accessToken, refreshToken, jwtProperties.expirationSeconds());
    }

    private String persistRefreshToken(UUID userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
        token.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.refreshExpirationSeconds()));
        return refreshTokenRepository.save(token).getToken();
    }

    private AuthenticatedUser toPrincipal(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).map(Enum::name).toList();
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getEmpresaId(),
                roles, user.getClienteId());
    }
}
