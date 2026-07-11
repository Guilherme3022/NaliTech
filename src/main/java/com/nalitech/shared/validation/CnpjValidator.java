package com.nalitech.shared.validation;

public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    public static String normalize(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "");
    }

    public static boolean isValid(String rawCnpj) {
        String cnpj = normalize(rawCnpj);
        if (cnpj == null || cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int firstDigit = checkDigit(cnpj, WEIGHTS_FIRST);
        int secondDigit = checkDigit(cnpj, WEIGHTS_SECOND);
        return firstDigit == charToInt(cnpj, 12) && secondDigit == charToInt(cnpj, 13);
    }

    private static int checkDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += charToInt(cnpj, i) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int charToInt(String value, int index) {
        return value.charAt(index) - '0';
    }
}
