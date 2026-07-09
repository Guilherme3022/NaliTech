package com.ledgerflow.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerflow.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
