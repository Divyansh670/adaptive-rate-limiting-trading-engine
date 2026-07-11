package com.tradingengine.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates an unreliable downstream dependency (e.g. an external exchange
 * connection) that we call through the circuit breaker. Fails a configurable
 * percentage of the time, on demand, to let us test breaker behavior.
 */
@Component
public class SimulatedExchangeClient {

    private volatile boolean forceFailure = false;

    public void setForceFailure(boolean forceFailure) {
        this.forceFailure = forceFailure;
    }

    public String callExchange() {
        if (forceFailure) {
            throw new RuntimeException("Simulated downstream exchange failure");
        }
        return "Exchange acknowledged";
    }
}