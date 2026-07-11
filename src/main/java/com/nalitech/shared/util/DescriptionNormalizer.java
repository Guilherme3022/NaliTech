package com.nalitech.shared.util;

import java.text.Normalizer;

/**
 * Normaliza descricoes de lancamentos para comparacao/aprendizado:
 * minusculas, sem acentos, removendo numeros/datas/pontuacao (partes variaveis)
 * e tokens de 1 caractere. Assim "PIX 12/03 DOC 998877" e "PIX 15/04 DOC 112233"
 * viram o mesmo padrao "pix doc".
 */
public final class DescriptionNormalizer {

    private static final int MAX_LENGTH = 200;

    private DescriptionNormalizer() {
    }

    public static String normalize(String descricao) {
        if (descricao == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(descricao.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // Tudo que nao for letra a-z vira espaco (remove digitos, datas, pontuacao).
        String soLetras = semAcento.replaceAll("[^a-z]+", " ");
        // Remove tokens de 1 caractere (ruido) e colapsa espacos.
        String limpo = soLetras.replaceAll("\\b\\w\\b", " ").replaceAll("\\s+", " ").trim();
        return limpo.length() > MAX_LENGTH ? limpo.substring(0, MAX_LENGTH) : limpo;
    }
}
