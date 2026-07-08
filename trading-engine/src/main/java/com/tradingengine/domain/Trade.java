package com.tradingengine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_order_id", nullable = false)
    private Order sellOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    protected Trade() {
    }

    public Trade(Order buyOrder, Order sellOrder, Symbol symbol, BigDecimal price, int quantity) {
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.executedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Order getBuyOrder() { return buyOrder; }
    public Order getSellOrder() { return sellOrder; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}