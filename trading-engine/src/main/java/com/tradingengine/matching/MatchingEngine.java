package com.tradingengine.matching;

import com.tradingengine.domain.*;
import com.tradingengine.repository.OrderRepository;
import com.tradingengine.repository.TradeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Matches an incoming order against the resting orders on the opposite side
 * of the book, producing Trades and updating order state as it goes.
 */
@Component
public class MatchingEngine {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;

    public MatchingEngine(OrderRepository orderRepository, TradeRepository tradeRepository) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
    }

    /**
     * Attempts to match the incoming order against the book.
     * Any unmatched remainder (for GTC orders) is added to the book to rest.
     * Returns the list of trades produced.
     */
    public List<Trade> match(Order incoming, OrderBook book) {
        List<Trade> trades = new ArrayList<>();
        boolean isBuy = incoming.getSide() == Side.BUY;

        while (incoming.getRemainingQuantity() > 0) {
            BigDecimal oppositeBestPrice = isBuy ? book.getBestAskPrice() : book.getBestBidPrice();
            if (oppositeBestPrice == null) {
                break; // nothing to match against
            }

            boolean pricesCross = isBuy
                    ? incoming.getPrice().compareTo(oppositeBestPrice) >= 0   // buyer willing to pay >= best ask
                    : incoming.getPrice().compareTo(oppositeBestPrice) <= 0;  // seller willing to accept <= best bid

            if (!pricesCross) {
                break; // best available price isn't good enough to match
            }

            Deque<Order> restingQueue = isBuy ? book.getOrdersAtBestAsk() : book.getOrdersAtBestBid();
            Order resting = restingQueue.peekFirst();

            int matchedQty = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());

            Order buyOrder = isBuy ? incoming : resting;
            Order sellOrder = isBuy ? resting : incoming;

            Trade trade = new Trade(buyOrder, sellOrder, incoming.getSymbol(), oppositeBestPrice, matchedQty);
            tradeRepository.save(trade);
            trades.add(trade);

            incoming.fill(matchedQty);
            resting.fill(matchedQty);
            orderRepository.save(incoming);
            orderRepository.save(resting);

            if (resting.getRemainingQuantity() == 0) {
                restingQueue.pollFirst(); // fully filled resting order leaves the book
            }
        }

        // Handle whatever remains of the incoming order based on its TIF
        if (incoming.getRemainingQuantity() > 0) {
            if (incoming.getTif() == TimeInForce.GTC) {
                incoming.markOpen();
                orderRepository.save(incoming);
                book.addOrder(incoming); // rest in the book, waiting for a future match
            } else {
                // IOC and FOK never rest in the book
                incoming.cancel();
                orderRepository.save(incoming);
            }
        }

        return trades;
    }
}