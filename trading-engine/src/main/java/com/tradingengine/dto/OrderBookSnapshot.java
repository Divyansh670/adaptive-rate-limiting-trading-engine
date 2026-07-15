package com.tradingengine.dto;

import java.math.BigDecimal;

public class OrderBookSnapshot {
    private String ticker;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private int totalOrders;

    public OrderBookSnapshot() {}

    public OrderBookSnapshot(String ticker, BigDecimal bestBid, BigDecimal bestAsk, int totalOrders) {
        this.ticker = ticker;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.totalOrders = totalOrders;
    }

    public String getTicker() { return ticker; }
    public BigDecimal getBestBid() { return bestBid; }
    public BigDecimal getBestAsk() { return bestAsk; }
    public int getTotalOrders() { return totalOrders; }

    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setBestBid(BigDecimal bestBid) { this.bestBid = bestBid; }
    public void setBestAsk(BigDecimal bestAsk) { this.bestAsk = bestAsk; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
}