package com.tradingengine.controller;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Symbol;
import com.tradingengine.domain.Trade;
import com.tradingengine.dto.CreateOrderRequest;
import com.tradingengine.matching.ExpiryQueue;
import com.tradingengine.matching.MatchingEngine;
import com.tradingengine.matching.OrderBook;
import com.tradingengine.matching.OrderBookRegistry;
import com.tradingengine.repository.OrderRepository;
import com.tradingengine.repository.SymbolRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final SymbolRepository symbolRepository;
    private final OrderRepository orderRepository;
    private final OrderBookRegistry orderBookRegistry;
    private final MatchingEngine matchingEngine;
    private final ExpiryQueue expiryQueue;

    public OrderController(SymbolRepository symbolRepository,
                            OrderRepository orderRepository,
                            OrderBookRegistry orderBookRegistry,
                            MatchingEngine matchingEngine,
                            ExpiryQueue expiryQueue) {
        this.symbolRepository = symbolRepository;
        this.orderRepository = orderRepository;
        this.orderBookRegistry = orderBookRegistry;
        this.matchingEngine = matchingEngine;
        this.expiryQueue = expiryQueue;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Symbol symbol = symbolRepository.findByTicker(request.getTicker())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + request.getTicker()));

        Order order = new Order(symbol, request.getSide(), request.getPrice(), request.getQuantity(), request.getTif());

        if (request.getTtlSeconds() != null) {
            order.setExpiresAt(LocalDateTime.now().plusSeconds(request.getTtlSeconds()));
        }

        Order saved = orderRepository.save(order);

        OrderBook book = orderBookRegistry.getOrCreate(symbol.getTicker());
        List<Trade> trades = matchingEngine.match(saved, book);

        if (saved.getExpiresAt() != null && saved.getStatus() == com.tradingengine.domain.OrderStatus.OPEN) {
            expiryQueue.schedule(saved);
        }

        return Map.of(
                "orderId", saved.getId(),
                "ticker", symbol.getTicker(),
                "side", saved.getSide(),
                "price", saved.getPrice(),
                "quantity", saved.getQuantity(),
                "status", saved.getStatus(),
                "filledQuantity", saved.getFilledQuantity(),
                "tradesExecuted", trades.size()
        );
    }

    @GetMapping("/book/{ticker}")
    public Map<String, Object> viewBook(@PathVariable String ticker) {
        OrderBook book = orderBookRegistry.getOrCreate(ticker);
        Map<String, Object> response = new HashMap<>();
        response.put("ticker", ticker);
        response.put("bestBid", book.getBestBidPrice());
        response.put("bestAsk", book.getBestAskPrice());
        response.put("totalOrders", book.totalOrderCount());
        return response;
    }
}