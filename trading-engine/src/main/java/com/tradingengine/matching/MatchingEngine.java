package com.tradingengine.matching;

import com.tradingengine.domain.*;
import com.tradingengine.dto.OrderBookSnapshot;
import com.tradingengine.dto.TradeEvent;
import com.tradingengine.kafka.TradeEventProducer;
import com.tradingengine.ratelimit.VolatilityTracker;
import com.tradingengine.repository.OrderRepository;
import com.tradingengine.repository.TradeRepository;
import com.tradingengine.websocket.DashboardBroadcastService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private final VolatilityTracker volatilityTracker;
    private final TradeEventProducer tradeEventProducer;
    private final DashboardBroadcastService broadcastService;

    public MatchingEngine(OrderRepository orderRepository,
                           TradeRepository tradeRepository,
                           VolatilityTracker volatilityTracker,
                           TradeEventProducer tradeEventProducer,
                           DashboardBroadcastService broadcastService) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.volatilityTracker = volatilityTracker;
        this.tradeEventProducer = tradeEventProducer;
        this.broadcastService = broadcastService;
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
                break;
            }

            boolean pricesCross = isBuy
                    ? incoming.getPrice().compareTo(oppositeBestPrice) >= 0
                    : incoming.getPrice().compareTo(oppositeBestPrice) <= 0;

            if (!pricesCross) {
                break;
            }

            Deque<Order> restingQueue = isBuy ? book.getOrdersAtBestAsk() : book.getOrdersAtBestBid();
            Order resting = restingQueue.peekFirst();

            int matchedQty = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());

            Order buyOrder = isBuy ? incoming : resting;
            Order sellOrder = isBuy ? resting : incoming;

            Trade trade = new Trade(buyOrder, sellOrder, incoming.getSymbol(), oppositeBestPrice, matchedQty);
            tradeRepository.save(trade);
            trades.add(trade);
            volatilityTracker.recordTrade(trade);

            TradeEvent event = new TradeEvent(
                    trade.getId(), incoming.getSymbol().getTicker(),
                    buyOrder.getId(), sellOrder.getId(),
                    trade.getPrice(), trade.getQuantity(), trade.getExecutedAt()
            );
            tradeEventProducer.publish(event);
            broadcastService.broadcastTrade(event);

            incoming.fill(matchedQty);
            resting.fill(matchedQty);
            orderRepository.save(incoming);
            orderRepository.save(resting);

            if (resting.getRemainingQuantity() == 0) {
                book.removeOrder(resting);
            }

            broadcastService.broadcastOrderBook(new OrderBookSnapshot(
                    incoming.getSymbol().getTicker(), book.getBestBidPrice(), book.getBestAskPrice(), book.totalOrderCount()
            ));
        }

        if (incoming.getRemainingQuantity() > 0) {
            if (incoming.getTif() == TimeInForce.GTC) {
                incoming.markOpen();
                orderRepository.save(incoming);
                book.addOrder(incoming);
            } else {
                incoming.cancel();
                orderRepository.save(incoming);
            }
        }

        return trades;
    }
}