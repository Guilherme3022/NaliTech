package com.nalitech.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalitech.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
