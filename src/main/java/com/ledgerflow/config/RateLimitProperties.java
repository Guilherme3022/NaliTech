package com.ledgerflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerflow.rate-limit")
public record RateLimitProperties(
        long capacity,
        long refillPeriodSeconds
) {
}
