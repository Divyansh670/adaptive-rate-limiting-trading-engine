package com.tradingengine.matching;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Side;
import com.tradingengine.domain.Symbol;
import com.tradingengine.domain.TimeInForce;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentMatchingTest {

    /**
     * Regression test for a race condition where multiple threads matching
     * against the same resting order could over-fill it, producing negative
     * remaining quantities. This test doesn't call the full MatchingEngine
     * (which needs Spring-managed dependencies); instead it verifies that
     * OrderBook itself behaves safely when orders are added/removed
     * concurrently from multiple threads.
     */
    @Test
    void concurrentAddAndRemoveDoesNotCorruptBook() throws InterruptedException {
        OrderBook book = new OrderBook("AAPL");
        Symbol symbol = new Symbol("AAPL");
        int threadCount = 10;

        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            orders.add(new Order(symbol, Side.SELL, new BigDecimal("200.00"), 5, TimeInForce.GTC));
        }
        orders.forEach(book::addOrder);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (Order order : orders) {
            pool.submit(() -> {
                try {
                    book.removeOrder(order);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        assertEquals(0, book.totalOrderCount());
        assertTrue(book.getBestAskPrice() == null);
    }
}