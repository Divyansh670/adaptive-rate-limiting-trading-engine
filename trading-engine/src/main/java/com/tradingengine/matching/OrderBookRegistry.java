package com.tradingengine.matching;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds one OrderBook per trading symbol, created on first use.
 */
@Component
public class OrderBookRegistry {

    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    public OrderBook getOrCreate(String ticker) {
        return books.computeIfAbsent(ticker, OrderBook::new);
    }
}