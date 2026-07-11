package com.nalitech.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CnpjValidatorTest {

    @Test
    void aceitaCnpjValidoComEMascara() {
        assertThat(CnpjValidator.isValid("11.222.333/0001-81")).isTrue();
        assertThat(CnpjValidator.isValid("11222333000181")).isTrue();
    }

    @Test
    void rejeitaCnpjComDigitoVerificadorErrado() {
        assertThat(CnpjValidator.isValid("11222333000180")).isFalse();
    }

    @Test
    void rejeitaCnpjComTodosDigitosIguaisOuTamanhoErrado() {
        assertThat(CnpjValidator.isValid("00000000000000")).isFalse();
        assertThat(CnpjValidator.isValid("123")).isFalse();
        assertThat(CnpjValidator.isValid(null)).isFalse();
    }
}
