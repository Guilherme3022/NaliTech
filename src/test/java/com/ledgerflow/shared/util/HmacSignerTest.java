package com.ledgerflow.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HmacSignerTest {

    @Test
    void geraAssinaturaDeterministicaEValidavel() {
        String payload = "{\"event\":\"cobranca.paga\"}";
        String secret = "segredo-do-webhook";

        String signature = HmacSigner.sign(payload, secret);

        assertThat(signature).hasSize(64);
        assertThat(HmacSigner.matches(payload, secret, signature)).isTrue();
    }

    @Test
    void assinaturaComSegredoErradoNaoConfere() {
        String payload = "{\"event\":\"cobranca.paga\"}";
        String signature = HmacSigner.sign(payload, "segredo-certo");

        assertThat(HmacSigner.matches(payload, "segredo-errado", signature)).isFalse();
    }
}
