package com.tradingengine.resilience;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple circuit breaker: after enough consecutive failures, trips OPEN
 * and rejects calls immediately for a cooldown period, then allows one
 * trial call (HALF_OPEN) to check if the downstream has recovered.
 */
@Component
public class CircuitBreaker {

    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration COOLDOWN = Duration.ofSeconds(10);

    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt;

    public boolean allowRequest() {
        if (state.get() == CircuitState.OPEN) {
            if (Duration.between(openedAt, Instant.now()).compareTo(COOLDOWN) >= 0) {
                state.set(CircuitState.HALF_OPEN);
                return true; // allow exactly one trial request through
            }
            return false;
        }
        return true; // CLOSED or HALF_OPEN both allow the request
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        state.set(CircuitState.CLOSED);
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (state.get() == CircuitState.HALF_OPEN || failures >= FAILURE_THRESHOLD) {
            state.set(CircuitState.OPEN);
            openedAt = Instant.now();
        }
    }

    public CircuitState getState() {
        return state.get();
    }
}