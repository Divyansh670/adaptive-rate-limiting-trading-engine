package com.tradingengine.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Sliding Window Log algorithm: records the exact timestamp of every request
 * in a Redis sorted set. Allows bursts, but strictly caps total requests
 * within any rolling time window (not just fixed windows).
 * Better suited to market-makers, who send bursts but must respect a hard cap.
 */
@Component
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final int MAX_REQUESTS = 50;
    private static final Duration WINDOW = Duration.ofSeconds(10);

    private final StringRedisTemplate redis;

    public SlidingWindowRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String clientId) {
        String key = "ratelimit:slidingwindow:" + clientId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW.toMillis();

        ZSetOperations<String, String> zSet = redis.opsForZSet();

        // Drop entries older than the window
        zSet.removeRangeByScore(key, 0, windowStart);

        Long currentCount = zSet.zCard(key);
        if (currentCount != null && currentCount >= MAX_REQUESTS) {
            return false;
        }

        // Record this request with a unique member (timestamp alone could collide)
        zSet.add(key, UUID.randomUUID().toString(), now);
        redis.expire(key, WINDOW);

        return true;
    }
}