package com.nalitech.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nalitech.config.JwtProperties;
import com.nalitech.modules.auth.dto.AuthDtos.LoginResponse;
import com.nalitech.modules.auth.entity.RefreshToken;
import com.nalitech.modules.auth.repository.RefreshTokenRepository;
import com.nalitech.modules.user.entity.User;
import com.nalitech.modules.user.entity.UserStatus;
import com.nalitech.modules.user.repository.UserRepository;
import com.nalitech.security.JwtService;
import com.nalitech.shared.exception.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "segredo-de-teste-com-tamanho-suficiente-para-hs256", 3600, 2592000);
        jwtService = new JwtService(jwtProperties);
        authService = new AuthService(userRepository, refreshTokenRepository,
                passwordEncoder, jwtService, jwtProperties);
    }

    @Test
    void loginComCredenciaisValidasEmiteTokens() {
        User user = ativoComSenha("segredo123");
        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.login("ana@x.com", "segredo123");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void loginComSenhaErradaFalha() {
        User user = ativoComSenha("segredo123");
        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("ana@x.com", "errada"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void loginDeUsuarioInexistenteFalha() {
        when(userRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nao@existe.com", "qualquer"))
                .isInstanceOf(BusinessException.class);
    }

    private User ativoComSenha(String senha) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmpresaId(UUID.randomUUID());
        user.setEmail("ana@x.com");
        user.setPasswordHash(passwordEncoder.encode(senha));
        user.setStatus(UserStatus.ATIVO);
        return user;
    }
}
