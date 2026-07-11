package com.tradingengine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeEvent {

    private Long tradeId;
    private String ticker;
    private Long buyOrderId;
    private Long sellOrderId;
    private BigDecimal price;
    private int quantity;
    private LocalDateTime executedAt;

    public TradeEvent() {
        // required for JSON deserialization
    }

    public TradeEvent(Long tradeId, String ticker, Long buyOrderId, Long sellOrderId,
                       BigDecimal price, int quantity, LocalDateTime executedAt) {
        this.tradeId = tradeId;
        this.ticker = ticker;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.executedAt = executedAt;
    }

    public Long getTradeId() { return tradeId; }
    public String getTicker() { return ticker; }
    public Long getBuyOrderId() { return buyOrderId; }
    public Long getSellOrderId() { return sellOrderId; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getExecutedAt() { return executedAt; }

    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setBuyOrderId(Long buyOrderId) { this.buyOrderId = buyOrderId; }
    public void setSellOrderId(Long sellOrderId) { this.sellOrderId = sellOrderId; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}