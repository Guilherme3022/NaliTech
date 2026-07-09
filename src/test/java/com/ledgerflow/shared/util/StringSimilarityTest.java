package com.ledgerflow.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringSimilarityTest {

    @Test
    void textosIdenticosRetornamUm() {
        assertThat(StringSimilarity.ratio("Pagamento Fornecedor", "pagamento fornecedor"))
                .isEqualTo(1.0);
    }

    @Test
    void textosParecidosRetornamAlto() {
        assertThat(StringSimilarity.ratio("Tarifa bancaria", "Tarifa bancária mensal"))
                .isGreaterThan(0.5);
    }

    @Test
    void textosDiferentesRetornamBaixo() {
        assertThat(StringSimilarity.ratio("PIX recebido", "Compra cartao"))
                .isLessThan(0.5);
    }
}
