package com.nalitech.shared.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class StringSimilarity {

    private StringSimilarity() {
    }

    /**
     * Similaridade por palavras (indice de Jaccard): |A ∩ B| / |A ∪ B| dos tokens.
     * Mais robusta que a distancia de caracteres para descricoes com partes
     * variaveis. Espera strings ja normalizadas (tokens separados por espaco).
     */
    public static double tokenSimilarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        Set<String> setA = tokens(a);
        Set<String> setB = tokens(b);
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersecao = new HashSet<>(setA);
        intersecao.retainAll(setB);
        Set<String> uniao = new HashSet<>(setA);
        uniao.addAll(setB);
        return (double) intersecao.size() / uniao.size();
    }

    /**
     * Coeficiente de sobreposicao: |A ∩ B| / min(|A|, |B|). Ao contrario do Jaccard, NAO
     * penaliza quando um lado tem muito mais tokens que o outro — ideal para casar uma
     * descricao de lancamento (com ruido) contra o nome de uma conta/razao social
     * (ex.: "black decker" x "black decker brasil ltda").
     */
    public static double tokenOverlap(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        Set<String> setA = tokens(a);
        Set<String> setB = tokens(b);
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersecao = new HashSet<>(setA);
        intersecao.retainAll(setB);
        int min = Math.min(setA.size(), setB.size());
        return (double) intersecao.size() / min;
    }

    /** Quantidade de tokens em comum entre as duas strings (ja normalizadas). */
    public static int commonTokenCount(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        Set<String> setA = tokens(a);
        setA.retainAll(tokens(b));
        return setA.size();
    }

    /** Numero de tokens da string (ja normalizada). */
    public static int tokenCount(String a) {
        return a == null ? 0 : tokens(a).size();
    }

    /**
     * Dois tokens sao "muito parecidos" quando: iguais; um e prefixo do outro (>=4 chars,
     * ex.: {@code distribuidora}/{@code distrib}); ou tem razao de similaridade >= 0.8
     * (ex.: {@code decker}/{@code deckers}). Tokens curtos (<4) so contam se iguais.
     */
    public static boolean tokensSimilares(String x, String y) {
        if (x == null || y == null) {
            return false;
        }
        if (x.equals(y)) {
            return true;
        }
        if (Math.min(x.length(), y.length()) < 4) {
            return false;
        }
        if (x.startsWith(y) || y.startsWith(x)) {
            return true;
        }
        return ratio(x, y) >= 0.8;
    }

    /** Conta tokens de A que tem um token IGUAL OU MUITO PARECIDO em B (sem reusar par). */
    public static int commonOrSimilarTokenCount(String a, String b) {
        List<String> tb = new ArrayList<>(tokenList(b));
        int count = 0;
        for (String ta : tokenList(a)) {
            for (Iterator<String> it = tb.iterator(); it.hasNext();) {
                if (tokensSimilares(ta, it.next())) {
                    it.remove();
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /** Coeficiente de sobreposicao FUZZY: |iguais ou parecidos| / min(|A|,|B|). */
    public static double tokenOverlapFuzzy(String a, String b) {
        int min = Math.min(tokenCount(a), tokenCount(b));
        if (min == 0) {
            return 0.0;
        }
        return (double) commonOrSimilarTokenCount(a, b) / min;
    }

    private static List<String> tokenList(String value) {
        List<String> tokens = new ArrayList<>();
        if (value == null) {
            return tokens;
        }
        for (String token : value.trim().split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static Set<String> tokens(String value) {
        Set<String> tokens = new HashSet<>();
        for (String token : value.trim().split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public static double ratio(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        String x = a.trim().toLowerCase();
        String y = b.trim().toLowerCase();
        if (x.isEmpty() && y.isEmpty()) {
            return 1.0;
        }
        int distance = levenshtein(x, y);
        int maxLen = Math.max(x.length(), y.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[b.length()];
    }
}
