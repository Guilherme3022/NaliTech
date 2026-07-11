package com.nalitech.security;

final class TokenBucket {

    private final long capacity;
    private final double refillPerMilli;

    private double tokens;
    private long lastRefillMillis;

    TokenBucket(long capacity, long refillPeriodMillis) {
        this.capacity = capacity;
        this.refillPerMilli = (double) capacity / refillPeriodMillis;
        this.tokens = capacity;
        this.lastRefillMillis = System.currentTimeMillis();
    }

    synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillMillis;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * refillPerMilli);
            lastRefillMillis = now;
        }
    }
}
