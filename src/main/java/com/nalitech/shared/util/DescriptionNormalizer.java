package com.nalitech.shared.util;

import java.text.Normalizer;
import java.util.Set;

/**
 * Normaliza descricoes de lancamentos para comparacao/aprendizado:
 * minusculas, sem acentos, removendo numeros/datas/pontuacao (partes variaveis)
 * e tokens de 1 caractere. Assim "PIX 12/03 DOC 998877" e "PIX 15/04 DOC 112233"
 * viram o mesmo padrao "pix doc".
 */
public final class DescriptionNormalizer {

    private static final int MAX_LENGTH = 200;

    // Termos de tipo de operacao bancaria que NAO identificam a contraparte. Removidos
    // para o padrao focar em quem pagou/recebeu (ex.: "pix recebido nestle" -> "nestle"),
    // evitando casar dois PIX de partes diferentes so por serem "pix".
    private static final Set<String> STOPWORDS = Set.of(
            "pix", "ted", "doc", "tef", "tev", "pagamento", "pagto", "pag", "recebido",
            "recebimento", "boleto", "cred", "deb", "credito", "debito", "transferencia",
            "transf", "cartao", "deposito", "saque", "tarifa", "cobranca", "liquid", "princ",
            "conta", "corrente", "cessao", "fornecedor", "cliente", "nf", "nfe", "serie",
            "ltda", "sa", "me", "epp", "eireli", "de", "da", "do", "dos", "das", "em");

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
        // Remove os termos genericos de operacao, mantendo so a contraparte.
        StringBuilder sb = new StringBuilder();
        for (String token : limpo.split("\\s+")) {
            if (!token.isBlank() && !STOPWORDS.contains(token)) {
                sb.append(sb.length() > 0 ? " " : "").append(token);
            }
        }
        // Se sobrou vazio (descricao so tinha termos genericos), preserva o padrao limpo
        // para nao perder totalmente a informacao.
        String resultado = sb.length() > 0 ? sb.toString() : limpo;
        return resultado.length() > MAX_LENGTH ? resultado.substring(0, MAX_LENGTH) : resultado;
    }
}
