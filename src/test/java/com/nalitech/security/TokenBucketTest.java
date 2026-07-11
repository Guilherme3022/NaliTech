package com.nalitech.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void permiteConsumirAteACapacidade() {
        TokenBucket bucket = new TokenBucket(3, 10_000);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
    }

    @Test
    void bloqueiaQuandoOsTokensAcabam() {
        TokenBucket bucket = new TokenBucket(2, 10_000);
        bucket.tryConsume();
        bucket.tryConsume();

        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void reabasteceAposOPeriodo() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(2, 100);
        bucket.tryConsume();
        bucket.tryConsume();
        assertThat(bucket.tryConsume()).isFalse();

        Thread.sleep(150);

        assertThat(bucket.tryConsume()).isTrue();
    }
}
