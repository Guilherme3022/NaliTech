package com.nalitech.modules.account.entity;

import java.text.Normalizer;
import java.util.Set;

/**
 * Natureza estrutural de uma conta do plano de contas:
 * <ul>
 *   <li>{@link #SINTETICA} — conta agrupadora/totalizadora; <b>nao</b> recebe lancamentos;</li>
 *   <li>{@link #ANALITICA} — conta "folha", lancavel (a unica elegivel na conciliacao);</li>
 *   <li>{@link #INDEFINIDA} — o arquivo nao informou o tipo (pode ser inferido pela hierarquia).</li>
 * </ul>
 *
 * <p>Aceita os varios rotulos que os sistemas contabeis usam para o mesmo conceito:
 * {@code S}/{@code A} (layout legado e SPED ECD I050), {@code SINTETICA}/{@code ANALITICA},
 * {@code 1}/{@code 2} (nivel de agregacao), {@code T}/{@code TOTAL} etc.</p>
 */
public enum ChartAccountKind {

    SINTETICA,
    ANALITICA,
    INDEFINIDA;

    private static final Set<String> SINTETICA_TOKENS =
            Set.of("S", "SINTETICA", "SINTETICO", "1", "T", "TOTAL", "TITULO", "GRUPO", "G");
    private static final Set<String> ANALITICA_TOKENS =
            Set.of("A", "ANALITICA", "ANALITICO", "2", "L", "LANCAMENTO", "MOVIMENTO", "M");

    /** {@code Boolean} correspondente para persistir/consultar: analitica? (null quando indefinida). */
    public Boolean analitica() {
        return switch (this) {
            case ANALITICA -> Boolean.TRUE;
            case SINTETICA -> Boolean.FALSE;
            case INDEFINIDA -> null;
        };
    }

    /** Rotulo canonico para gravar em {@code chart_of_accounts.tipo} (null quando indefinida). */
    public String label() {
        return this == INDEFINIDA ? null : name();
    }

    /** Interpreta um rotulo cru (S/A, sintetica/analitica, 1/2, ...) tolerando acentos e caixa. */
    public static ChartAccountKind normalize(String raw) {
        if (raw == null) {
            return INDEFINIDA;
        }
        String token = stripAccents(raw).trim().toUpperCase();
        if (token.isEmpty()) {
            return INDEFINIDA;
        }
        if (SINTETICA_TOKENS.contains(token)) {
            return SINTETICA;
        }
        if (ANALITICA_TOKENS.contains(token)) {
            return ANALITICA;
        }
        return INDEFINIDA;
    }

    /** Deriva o {@code Boolean analitica} a partir de um {@code Boolean} + rotulo cru (fallback). */
    public static Boolean resolveAnalitica(Boolean explicit, String rawTipo) {
        if (explicit != null) {
            return explicit;
        }
        return normalize(rawTipo).analitica();
    }

    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
