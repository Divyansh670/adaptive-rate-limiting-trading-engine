package com.tradingengine.controller;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.OrderStatus;
import com.tradingengine.domain.Symbol;
import com.tradingengine.dto.CreateOrderRequest;
import com.tradingengine.matching.OrderBook;
import com.tradingengine.matching.OrderBookRegistry;
import com.tradingengine.repository.OrderRepository;
import com.tradingengine.repository.SymbolRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final SymbolRepository symbolRepository;
    private final OrderRepository orderRepository;
    private final OrderBookRegistry orderBookRegistry;

    public OrderController(SymbolRepository symbolRepository,
                            OrderRepository orderRepository,
                            OrderBookRegistry orderBookRegistry) {
        this.symbolRepository = symbolRepository;
        this.orderRepository = orderRepository;
        this.orderBookRegistry = orderBookRegistry;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Symbol symbol = symbolRepository.findByTicker(request.getTicker())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + request.getTicker()));

        Order order = new Order(symbol, request.getSide(), request.getPrice(), request.getQuantity(), request.getTif());
        order.markOpen();

        Order saved = orderRepository.save(order);

        OrderBook book = orderBookRegistry.getOrCreate(symbol.getTicker());
        book.addOrder(saved);

        return Map.of(
                "orderId", saved.getId(),
                "ticker", symbol.getTicker(),
                "side", saved.getSide(),
                "price", saved.getPrice(),
                "quantity", saved.getQuantity(),
                "status", saved.getStatus()
        );
    }

    @GetMapping("/book/{ticker}")
    public Map<String, Object> viewBook(@PathVariable String ticker) {
        OrderBook book = orderBookRegistry.getOrCreate(ticker);
        return Map.of(
                "ticker", ticker,
                "bestBid", book.getBestBidPrice(),
                "bestAsk", book.getBestAskPrice(),
                "totalOrders", book.totalOrderCount()
        );
    }
}