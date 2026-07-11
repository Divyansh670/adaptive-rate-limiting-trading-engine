package com.tradingengine.ratelimit;

import com.tradingengine.domain.Trade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tracks recent trade prices to compute a simple volatility signal:
 * the percentage swing between the highest and lowest price in the
 * last N trades. Used to tighten rate limits automatically when the
 * market is moving sharply.
 */
@Component
public class VolatilityTracker {

    private static final int WINDOW_SIZE = 20;
    private static final BigDecimal HIGH_VOLATILITY_THRESHOLD = new BigDecimal("0.03"); // 3% swing

    private final ConcurrentLinkedDeque<BigDecimal> recentPrices = new ConcurrentLinkedDeque<>();

    public void recordTrade(Trade trade) {
        recentPrices.addLast(trade.getPrice());
        while (recentPrices.size() > WINDOW_SIZE) {
            recentPrices.pollFirst();
        }
    }

    public boolean isHighVolatility() {
        if (recentPrices.size() < 2) {
            return false;
        }
        BigDecimal min = recentPrices.stream().min(BigDecimal::compareTo).orElseThrow();
        BigDecimal max = recentPrices.stream().max(BigDecimal::compareTo).orElseThrow();

        if (min.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal swing = max.subtract(min).divide(min, 4, java.math.RoundingMode.HALF_UP);
        return swing.compareTo(HIGH_VOLATILITY_THRESHOLD) >= 0;
    }
}