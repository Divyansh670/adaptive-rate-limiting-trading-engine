package com.tradingengine.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RateLimitService {

    private final Map<ClientTier, RateLimiter> strategies;

    public RateLimitService(TokenBucketRateLimiter tokenBucket, SlidingWindowRateLimiter slidingWindow) {
        this.strategies = Map.of(
                ClientTier.RETAIL, tokenBucket,
                ClientTier.MARKET_MAKER, slidingWindow
        );
    }

    public boolean isAllowed(ClientTier tier, String clientId) {
        RateLimiter limiter = strategies.get(tier);
        return limiter.tryAcquire(clientId);
    }
}