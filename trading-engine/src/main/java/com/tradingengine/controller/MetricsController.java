package com.tradingengine.controller;

import com.tradingengine.metrics.MetricsTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MetricsController {

    private final MetricsTracker metricsTracker;

    public MetricsController(MetricsTracker metricsTracker) {
        this.metricsTracker = metricsTracker;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return Map.of(
                "totalOrders", metricsTracker.getTotalOrders(),
                "totalTrades", metricsTracker.getTotalTrades(),
                "rateLimitRejections", metricsTracker.getRateLimitRejections(),
                "avgMatchingLatencyMicros", String.format("%.2f", metricsTracker.getAvgMatchingLatencyMicros()),
                "ordersPerSecond", String.format("%.2f", metricsTracker.getOrdersPerSecond())
        );
    }
}