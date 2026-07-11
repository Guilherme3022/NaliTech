package com.nalitech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalitech.rate-limit")
public record RateLimitProperties(
        long capacity,
        long refillPeriodSeconds
) {
}
