package com.tradingengine.controller;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.OrderStatus;
import com.tradingengine.domain.Symbol;
import com.tradingengine.domain.Trade;
import com.tradingengine.dto.CreateOrderRequest;
import com.tradingengine.matching.MatchingEngine;
import com.tradingengine.matching.OrderBook;
import com.tradingengine.matching.OrderBookRegistry;
import com.tradingengine.repository.OrderRepository;
import com.tradingengine.repository.SymbolRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import com.tradingengine.domain.Trade;

import java.util.HashMap;
import java.util.List;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final SymbolRepository symbolRepository;
    private final OrderRepository orderRepository;
    private final OrderBookRegistry orderBookRegistry;

   private final MatchingEngine matchingEngine;

    public OrderController(SymbolRepository symbolRepository,
                            OrderRepository orderRepository,
                            OrderBookRegistry orderBookRegistry,
                            MatchingEngine matchingEngine) {
        this.symbolRepository = symbolRepository;
        this.orderRepository = orderRepository;
        this.orderBookRegistry = orderBookRegistry;
        this.matchingEngine = matchingEngine;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Symbol symbol = symbolRepository.findByTicker(request.getTicker())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + request.getTicker()));

        Order order = new Order(symbol, request.getSide(), request.getPrice(), request.getQuantity(), request.getTif());
        Order saved = orderRepository.save(order);

        OrderBook book = orderBookRegistry.getOrCreate(symbol.getTicker());
        List<Trade> trades = matchingEngine.match(saved, book);

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