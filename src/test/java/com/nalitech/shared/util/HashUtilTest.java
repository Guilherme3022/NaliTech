package com.nalitech.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HashUtilTest {

    @Test
    void sha256DeConteudoConhecido() {
        String hash = HashUtil.sha256Hex("abc".getBytes(StandardCharsets.UTF_8));

        assertThat(hash)
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sha256DeConteudoVazio() {
        String hash = HashUtil.sha256Hex(new byte[0]);

        assertThat(hash)
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void conteudosDiferentesGeramHashesDiferentes() {
        String a = HashUtil.sha256Hex("conteudo-a".getBytes(StandardCharsets.UTF_8));
        String b = HashUtil.sha256Hex("conteudo-b".getBytes(StandardCharsets.UTF_8));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashEhDeterministico() {
        byte[] conteudo = "extrato.ofx".getBytes(StandardCharsets.UTF_8);

        assertThat(HashUtil.sha256Hex(conteudo)).isEqualTo(HashUtil.sha256Hex(conteudo));
    }
}
