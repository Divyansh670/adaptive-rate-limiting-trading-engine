package com.tradingengine.controller;

import com.tradingengine.resilience.CircuitBreaker;
import com.tradingengine.resilience.SimulatedExchangeClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/circuit")
public class CircuitBreakerController {

    private final CircuitBreaker circuitBreaker;
    private final SimulatedExchangeClient exchangeClient;

    public CircuitBreakerController(CircuitBreaker circuitBreaker, SimulatedExchangeClient exchangeClient) {
        this.circuitBreaker = circuitBreaker;
        this.exchangeClient = exchangeClient;
    }

    @PostMapping("/toggle-failure")
    public Map<String, Object> toggleFailure(@RequestParam boolean fail) {
        exchangeClient.setForceFailure(fail);
        return Map.of("forceFailure", fail);
    }

    @GetMapping("/call")
    public Map<String, Object> call() {
        if (!circuitBreaker.allowRequest()) {
            return Map.of("result", "REJECTED_BY_BREAKER", "state", circuitBreaker.getState());
        }
        try {
            String result = exchangeClient.callExchange();
            circuitBreaker.recordSuccess();
            return Map.of("result", result, "state", circuitBreaker.getState());
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            return Map.of("result", "FAILED: " + e.getMessage(), "state", circuitBreaker.getState());
        }
    }
}