package com.nalitech.shared.util;

import java.util.HashSet;
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
