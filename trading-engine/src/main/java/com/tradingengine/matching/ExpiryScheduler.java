package com.tradingengine.matching;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.OrderStatus;
import com.tradingengine.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryScheduler {

    private final ExpiryQueue expiryQueue;
    private final OrderRepository orderRepository;
    private final OrderBookRegistry orderBookRegistry;

    public ExpiryScheduler(ExpiryQueue expiryQueue,
                            OrderRepository orderRepository,
                            OrderBookRegistry orderBookRegistry) {
        this.expiryQueue = expiryQueue;
        this.orderRepository = orderRepository;
        this.orderBookRegistry = orderBookRegistry;
    }

    /**
     * Runs every 2 seconds, popping and expiring any orders whose TTL has elapsed.
     */
    @Scheduled(fixedRate = 2000)
    public void expireOverdueOrders() {
        Order expired;
        while ((expired = expiryQueue.pollIfExpired()) != null) {
            if (expired.getStatus() != OrderStatus.OPEN && expired.getStatus() != OrderStatus.PARTIALLY_FILLED) {
                continue; // already filled or cancelled elsewhere, nothing to do
            }

            expired.expire();
            orderRepository.save(expired);

            OrderBook book = orderBookRegistry.getOrCreate(expired.getSymbol().getTicker());
            book.removeOrder(expired);
        }
    }
}