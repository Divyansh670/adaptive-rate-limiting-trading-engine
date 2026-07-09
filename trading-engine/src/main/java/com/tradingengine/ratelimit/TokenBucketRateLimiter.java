package com.tradingengine.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Token Bucket algorithm: each client gets a bucket that refills at a fixed rate.
 * Every request consumes one token. If the bucket is empty, the request is rejected.
 * Allows small bursts (up to bucket capacity) but enforces a steady average rate.
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static final int CAPACITY = 10;      // max tokens in the bucket
    private static final int REFILL_PER_WINDOW = 10; // tokens added per window
    private static final Duration WINDOW = Duration.ofSeconds(10); // refill window

    private final StringRedisTemplate redis;

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String clientId) {
        String key = "ratelimit:tokenbucket:" + clientId;

        // Simplified fixed-window approximation of token bucket:
        // increment a counter that resets every WINDOW; reject if it exceeds CAPACITY.
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            redis.expire(key, WINDOW); // start the window on the first request
        }
        return count <= CAPACITY;
    }
}