package com.tradingengine.matching;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Side;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * An in-memory limit order book for a single trading symbol.
 * <p>
 * Maintains two sides (bids and asks), each sorted by price-time priority:
 * - Bids (BUY orders) are sorted highest price first (best price to sell into).
 * - Asks (SELL orders) are sorted lowest price first (best price to buy from).
 * Within the same price level, orders are kept in a FIFO queue (earliest first).
 */
public class OrderBook {

    private final String ticker;

    // Bids: highest price first -> reverse natural ordering
    private final NavigableMap<BigDecimal, Deque<Order>> bids =
            new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    // Asks: lowest price first -> natural ordering
    private final NavigableMap<BigDecimal, Deque<Order>> asks =
            new ConcurrentSkipListMap<>();

    public OrderBook(String ticker) {
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }

    /**
     * Adds an order to the correct side of the book, at the correct price level,
     * appended to the end of that price level's queue (preserving time priority).
     */
    public void addOrder(Order order) {
        NavigableMap<BigDecimal, Deque<Order>> side = sideMapFor(order.getSide());

        // computeIfAbsent: if this price level doesn't exist yet, create an empty queue for it
        Deque<Order> ordersAtPrice = side.computeIfAbsent(order.getPrice(), price -> new ArrayDeque<>());
        ordersAtPrice.addLast(order); // addLast preserves FIFO / time priority
    }

    /**
     * Returns the best (highest) bid price currently in the book, or null if no bids exist.
     */
    public BigDecimal getBestBidPrice() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    /**
     * Returns the best (lowest) ask price currently in the book, or null if no asks exist.
     */
    public BigDecimal getBestAskPrice() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    /**
     * Returns the queue of orders sitting at the best bid price, or an empty queue if none.
     */
    public Deque<Order> getOrdersAtBestBid() {
        Map.Entry<BigDecimal, Deque<Order>> entry = bids.firstEntry();
        return entry == null ? new ArrayDeque<>() : entry.getValue();
    }

    /**
     * Returns the queue of orders sitting at the best ask price, or an empty queue if none.
     */
    public Deque<Order> getOrdersAtBestAsk() {
        Map.Entry<BigDecimal, Deque<Order>> entry = asks.firstEntry();
        return entry == null ? new ArrayDeque<>() : entry.getValue();
    }

    /**
     * Total number of resting orders currently in the book (both sides).
     */
    public int totalOrderCount() {
        int count = 0;
        for (Deque<Order> q : bids.values()) count += q.size();
        for (Deque<Order> q : asks.values()) count += q.size();
        return count;
    }

    /**
     * Removes a specific order from the book (used when an order expires, is cancelled,
     * or is fully filled during matching). Cleans up the price level entirely if it
     * becomes empty, preventing stale empty price levels from corrupting best-price lookups.
     */
    public boolean removeOrder(Order order) {
        NavigableMap<BigDecimal, Deque<Order>> side = sideMapFor(order.getSide());
        Deque<Order> ordersAtPrice = side.get(order.getPrice());
        if (ordersAtPrice == null) {
            return false;
        }
        boolean removed = ordersAtPrice.remove(order);
        if (ordersAtPrice.isEmpty()) {
            side.remove(order.getPrice());
        }
        return removed;
    }
    

    private NavigableMap<BigDecimal, Deque<Order>> sideMapFor(Side side) {
        return side == Side.BUY ? bids : asks;
    }
}