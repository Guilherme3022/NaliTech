package com.ledgerflow.modules.auth.service;

import com.ledgerflow.modules.auth.entity.PasswordResetToken;
import com.ledgerflow.modules.auth.repository.PasswordResetTokenRepository;
import com.ledgerflow.modules.auth.repository.RefreshTokenRepository;
import com.ledgerflow.modules.user.entity.User;
import com.ledgerflow.modules.user.repository.UserRepository;
import com.ledgerflow.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class PasswordResetService {

    private static final long TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository resetTokenRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void requestReset(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(OffsetDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
            resetTokenRepository.save(token);

            log.info("Solicitacao de reset de senha registrada para o usuario informado.");
            log.debug("Token de reset gerado para {}: {}", email, token.getToken());
        });
    }

    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = resetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException("Token invalido.", HttpStatus.BAD_REQUEST));
        if (!token.isUsable()) {
            throw new BusinessException("Token expirado ou ja utilizado.", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado.", HttpStatus.BAD_REQUEST));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        resetTokenRepository.save(token);

        refreshTokenRepository.revokeAllForUser(user.getId());
    }
}
