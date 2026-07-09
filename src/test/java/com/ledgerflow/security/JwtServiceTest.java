package com.ledgerflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-tamanho-suficiente-para-hs256";

    private JwtService serviceComExpiracao(long segundos) {
        return new JwtService(new JwtProperties(SECRET, segundos, 2592000));
    }

    private AuthenticatedUser usuario(UUID clienteId) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "ana@x.com",
                UUID.randomUUID(),
                List.of("ADMIN", "OPERADOR"),
                clienteId);
    }

    @Test
    void gerarEReinterpretarTokenPreservaClaims() {
        JwtService service = serviceComExpiracao(3600);
        AuthenticatedUser original = usuario(null);

        String token = service.generateAccessToken(original);
        Claims claims = service.parseClaims(token);
        AuthenticatedUser restaurado = service.toAuthenticatedUser(claims);

        assertThat(restaurado.id()).isEqualTo(original.id());
        assertThat(restaurado.email()).isEqualTo(original.email());
        assertThat(restaurado.empresaId()).isEqualTo(original.empresaId());
        assertThat(restaurado.roles()).containsExactlyInAnyOrder("ADMIN", "OPERADOR");
        assertThat(restaurado.clienteId()).isNull();
    }

    @Test
    void tokenComClienteIdPreservaOClaimOpcional() {
        JwtService service = serviceComExpiracao(3600);
        UUID clienteId = UUID.randomUUID();

        String token = service.generateAccessToken(usuario(clienteId));
        AuthenticatedUser restaurado = service.toAuthenticatedUser(service.parseClaims(token));

        assertThat(restaurado.clienteId()).isEqualTo(clienteId);
    }

    @Test
    void tokenValidoPassaNaValidacao() {
        JwtService service = serviceComExpiracao(3600);
        String token = service.generateAccessToken(usuario(null));

        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void tokenExpiradoNaoEhValido() {
        JwtService service = serviceComExpiracao(-10);
        String token = service.generateAccessToken(usuario(null));

        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void tokenAdulteradoNaoEhValido() {
        JwtService service = serviceComExpiracao(3600);
        String token = service.generateAccessToken(usuario(null));

        assertThat(service.isValid(token + "adulterado")).isFalse();
    }

    @Test
    void tokenAssinadoComOutraChaveNaoEhValido() {
        JwtService emissor = serviceComExpiracao(3600);
        JwtService outro = new JwtService(
                new JwtProperties("uma-chave-completamente-diferente-com-32b+", 3600, 2592000));
        String token = emissor.generateAccessToken(usuario(null));

        assertThat(outro.isValid(token)).isFalse();
    }
}
