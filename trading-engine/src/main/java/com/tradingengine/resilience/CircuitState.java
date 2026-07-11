package com.tradingengine.resilience;

public enum CircuitState {
    CLOSED,     // normal operation
    OPEN,       // failing, reject immediately
    HALF_OPEN   // testing if downstream recovered
}