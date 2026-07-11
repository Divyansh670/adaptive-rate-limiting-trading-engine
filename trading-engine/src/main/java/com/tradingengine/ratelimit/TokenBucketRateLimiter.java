package com.tradingengine.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static final int NORMAL_CAPACITY = 10;
    private static final int HIGH_VOLATILITY_CAPACITY = 4; // tightened under volatility
    private static final Duration WINDOW = Duration.ofSeconds(10);

    private final StringRedisTemplate redis;
    private final VolatilityTracker volatilityTracker;

    public TokenBucketRateLimiter(StringRedisTemplate redis, VolatilityTracker volatilityTracker) {
        this.redis = redis;
        this.volatilityTracker = volatilityTracker;
    }

    @Override
    public boolean tryAcquire(String clientId) {
        String key = "ratelimit:tokenbucket:" + clientId;
        int capacity = volatilityTracker.isHighVolatility() ? HIGH_VOLATILITY_CAPACITY : NORMAL_CAPACITY;

        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            redis.expire(key, WINDOW);
        }
        return count <= capacity;
    }
}