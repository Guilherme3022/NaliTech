package com.nalitech.modules.parser.model;

import java.util.List;
import java.util.Map;

public record ParseResult(
        List<RawMovement> movements,
        Map<String, String> metadata
) {
    public static ParseResult of(List<RawMovement> movements) {
        return new ParseResult(movements, Map.of());
    }
}
