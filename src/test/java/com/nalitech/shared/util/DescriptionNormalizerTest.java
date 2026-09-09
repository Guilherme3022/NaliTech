package com.nalitech.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class DescriptionNormalizerTest {

    @Test
    void removeNumerosDatasEDocumentos() {
        assertThat(DescriptionNormalizer.normalize("PIX 12/03 DOC 998877")).isEqualTo("pix doc");
        assertThat(DescriptionNormalizer.normalize("PIX 15/04 DOC 112233")).isEqualTo("pix doc");
    }

    @Test
    void removeAcentosEColapsaEspacos() {
        assertThat(DescriptionNormalizer.normalize("SALÁRIO   FUNCIONÁRIO")).isEqualTo("salario funcionario");
    }

    @Test
    void nuloOuSoNumerosViraVazio() {
        assertThat(DescriptionNormalizer.normalize(null)).isEmpty();
        assertThat(DescriptionNormalizer.normalize("12/03/2026 998877")).isEmpty();
    }

    @Test
    void focaNaContraparteRemovendoTermosGenericos() {
        // Termos de operacao (pix, recebido) sao removidos; sobra a contraparte.
        assertThat(DescriptionNormalizer.normalize("PIX RECEBIDO NESTLE BRASIL"))
                .isEqualTo("nestle brasil");
        assertThat(DescriptionNormalizer.normalize("PAGAMENTO DE BOLETO NESTLE BRASIL LTDA"))
                .isEqualTo("nestle brasil");
    }

    @Test
    void jaccardCasaDescricoesComPartesVariaveis() {
        String a = DescriptionNormalizer.normalize("PIX ENERGIA ELETRICA");
        String b = DescriptionNormalizer.normalize("PIX ENERGIA ELETRICA REF MAIO");
        // Sem os genericos: {energia,eletrica} vs {energia,eletrica,ref,maio} = 2/4
        assertThat(StringSimilarity.tokenSimilarity(a, b)).isCloseTo(0.5, within(0.001));
    }

    @Test
    void jaccardIgualParaMesmoConjuntoDeTermos() {
        String a = DescriptionNormalizer.normalize("TARIFA 01/05 pacote");
        String b = DescriptionNormalizer.normalize("tarifa pacote 09/06");
        assertThat(StringSimilarity.tokenSimilarity(a, b)).isEqualTo(1.0);
    }
}
