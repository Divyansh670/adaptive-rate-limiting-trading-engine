package com.tradingengine.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
public class MetricsTracker {

    private final LongAdder totalOrders = new LongAdder();
    private final LongAdder totalTrades = new LongAdder();
    private final LongAdder rateLimitRejections = new LongAdder();
    private final AtomicLong totalMatchingNanos = new AtomicLong(0);
    private final AtomicLong matchCount = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    public void recordOrder() {
        totalOrders.increment();
    }

    public void recordTrades(int count) {
        totalTrades.add(count);
    }

    public void recordRateLimitRejection() {
        rateLimitRejections.increment();
    }

    public void recordMatchingTime(long nanos) {
        totalMatchingNanos.addAndGet(nanos);
        matchCount.incrementAndGet();
    }

    public long getTotalOrders() {
        return totalOrders.sum();
    }

    public long getTotalTrades() {
        return totalTrades.sum();
    }

    public long getRateLimitRejections() {
        return rateLimitRejections.sum();
    }

    public double getAvgMatchingLatencyMicros() {
        long count = matchCount.get();
        if (count == 0) return 0;
        return (totalMatchingNanos.get() / (double) count) / 1000.0;
    }

    public double getOrdersPerSecond() {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsedSeconds == 0) return totalOrders.sum();
        return totalOrders.sum() / (double) elapsedSeconds;
    }
}