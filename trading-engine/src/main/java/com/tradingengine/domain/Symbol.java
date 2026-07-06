package com.tradingengine.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "symbol")
public class Symbol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Symbol() {
        // required by JPA
    }

    public Symbol(String ticker) {
        this.ticker = ticker;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}