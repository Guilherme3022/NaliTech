package com.nalitech.security;

import com.nalitech.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String EMPRESA_CLAIM = "empresaId";
    private static final String CLIENTE_CLAIM = "clienteId";
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey signingKey;
    private final long accessExpirationSeconds;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpirationSeconds = properties.expirationSeconds();
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.id().toString())
                .claim(EMAIL_CLAIM, user.email())
                .claim(ROLES_CLAIM, user.roles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
                .signWith(signingKey);
        // ADMIN geral fica "acima" das empresas (empresaId nulo).
        if (user.empresaId() != null) {
            builder.claim(EMPRESA_CLAIM, user.empresaId().toString());
        }
        if (user.clienteId() != null) {
            builder.claim(CLIENTE_CLAIM, user.clienteId().toString());
        }
        return builder.compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser toAuthenticatedUser(Claims claims) {
        UUID clienteId = claims.get(CLIENTE_CLAIM) != null
                ? UUID.fromString(claims.get(CLIENTE_CLAIM, String.class))
                : null;
        List<String> roles = claims.get(ROLES_CLAIM, List.class);
        UUID empresaId = claims.get(EMPRESA_CLAIM) != null
                ? UUID.fromString(claims.get(EMPRESA_CLAIM, String.class))
                : null;
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                empresaId,
                roles == null ? List.of() : roles,
                clienteId);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
