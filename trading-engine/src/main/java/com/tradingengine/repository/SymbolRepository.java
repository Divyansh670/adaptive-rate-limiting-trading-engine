package com.tradingengine.repository;

import com.tradingengine.domain.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SymbolRepository extends JpaRepository<Symbol, Long> {
    Optional<Symbol> findByTicker(String ticker);
}