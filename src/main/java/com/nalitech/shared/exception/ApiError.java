package com.nalitech.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fields
) {

    public record FieldError(String field, String message) {
    }
}
