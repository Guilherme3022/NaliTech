package com.ledgerflow.modules.auth.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern HAS_LETTER = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.length() >= 8
                && HAS_LETTER.matcher(value).matches()
                && HAS_DIGIT.matcher(value).matches();
    }
}
