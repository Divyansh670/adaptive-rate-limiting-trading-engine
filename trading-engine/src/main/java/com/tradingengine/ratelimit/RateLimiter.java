package com.tradingengine.ratelimit;

public interface RateLimiter {
    /**
     * Returns true if the request for this client is allowed to proceed,
     * false if it should be rejected (429).
     */
    boolean tryAcquire(String clientId);
}