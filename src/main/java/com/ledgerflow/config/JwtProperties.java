package com.ledgerflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerflow.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds,
        long refreshExpirationSeconds
) {
}
