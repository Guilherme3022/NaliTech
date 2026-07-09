package com.ledgerflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.config.RateLimitProperties;
import com.ledgerflow.shared.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_PATHS = List.of(
            "/auth/login", "/auth/forgot-password", "/auth/reset-password");

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PROTECTED_PATHS.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        TokenBucket bucket = buckets.computeIfAbsent(clientKey(request), key -> newBucket());
        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(request, response);
        }
    }

    private TokenBucket newBucket() {
        return new TokenBucket(properties.capacity(), properties.refillPeriodSeconds() * 1000);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ApiError body = new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Muitas requisicoes. Tente novamente em instantes.",
                request.getRequestURI(),
                null);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
