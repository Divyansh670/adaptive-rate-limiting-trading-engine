package com.tradingengine.matching;

import com.tradingengine.domain.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * A thread-safe min-heap of orders sorted by their expiration time.
 * The order expiring soonest is always at the head.
 */
@Component
public class ExpiryQueue {

    private final PriorityQueue<Order> heap =
            new PriorityQueue<>(Comparator.comparing(Order::getExpiresAt));

    public synchronized void schedule(Order order) {
        heap.offer(order);
    }

    /**
     * Removes and returns the order expiring soonest, but only if it has
     * actually expired (expiresAt <= now). Returns null if the heap is
     * empty or the earliest order hasn't expired yet.
     */
    public synchronized Order pollIfExpired() {
        Order head = heap.peek();
        if (head == null || head.getExpiresAt().isAfter(LocalDateTime.now())) {
            return null;
        }
        return heap.poll();
    }

    public synchronized int size() {
        return heap.size();
    }
}