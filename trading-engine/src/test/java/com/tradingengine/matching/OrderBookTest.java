package com.tradingengine.matching;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Side;
import com.tradingengine.domain.Symbol;
import com.tradingengine.domain.TimeInForce;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderBookTest {

    @Test
    void bestBidIsHighestPrice() {
        OrderBook book = new OrderBook("AAPL");
        Symbol symbol = new Symbol("AAPL");

        book.addOrder(new Order(symbol, Side.BUY, new BigDecimal("150.00"), 10, TimeInForce.GTC));
        book.addOrder(new Order(symbol, Side.BUY, new BigDecimal("151.50"), 5, TimeInForce.GTC));
        book.addOrder(new Order(symbol, Side.BUY, new BigDecimal("149.00"), 20, TimeInForce.GTC));

        assertEquals(new BigDecimal("151.50"), book.getBestBidPrice());
    }

    @Test
    void bestAskIsLowestPrice() {
        OrderBook book = new OrderBook("AAPL");
        Symbol symbol = new Symbol("AAPL");

        book.addOrder(new Order(symbol, Side.SELL, new BigDecimal("155.00"), 8, TimeInForce.GTC));
        book.addOrder(new Order(symbol, Side.SELL, new BigDecimal("153.25"), 3, TimeInForce.GTC));
        book.addOrder(new Order(symbol, Side.SELL, new BigDecimal("160.00"), 12, TimeInForce.GTC));

        assertEquals(new BigDecimal("153.25"), book.getBestAskPrice());
    }

    @Test
    void ordersAtSamePriceKeepFifoOrder() {
        OrderBook book = new OrderBook("AAPL");
        Symbol symbol = new Symbol("AAPL");

        Order first = new Order(symbol, Side.BUY, new BigDecimal("150.00"), 10, TimeInForce.GTC);
        Order second = new Order(symbol, Side.BUY, new BigDecimal("150.00"), 5, TimeInForce.GTC);

        book.addOrder(first);
        book.addOrder(second);

        Order headOfQueue = book.getOrdersAtBestBid().peekFirst();
        assertEquals(first, headOfQueue);
    }

    @Test
    void emptyBookHasNullBestPrices() {
        OrderBook book = new OrderBook("AAPL");

        assertEquals(null, book.getBestBidPrice());
        assertEquals(null, book.getBestAskPrice());
    }
}