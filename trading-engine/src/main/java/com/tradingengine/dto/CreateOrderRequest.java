package com.tradingengine.dto;

import com.tradingengine.domain.Side;
import com.tradingengine.domain.TimeInForce;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateOrderRequest {

    @NotBlank
    private String ticker;

    @NotNull
    private Side side;

    @NotNull
    @DecimalMin(value = "0.0001", message = "price must be positive")
    private BigDecimal price;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;

    @NotNull
    private TimeInForce tif;

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public TimeInForce getTif() {
        return tif;
    }

    public void setTif(TimeInForce tif) {
        this.tif = tif;
    }
    private Integer ttlSeconds; // optional; null means no expiry

    public Integer getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}