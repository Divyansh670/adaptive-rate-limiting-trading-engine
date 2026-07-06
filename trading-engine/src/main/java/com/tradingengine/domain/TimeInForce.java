package com.tradingengine.domain;

public enum TimeInForce {
    GTC,  // Good Till Cancel - stays in the book until explicitly cancelled
    IOC,  // Immediate Or Cancel - fill whatever is possible immediately, cancel the rest
    FOK   // Fill Or Kill - fill the entire order immediately, or cancel all of it
}