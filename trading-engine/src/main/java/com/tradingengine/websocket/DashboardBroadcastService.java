package com.tradingengine.websocket;

import com.tradingengine.dto.OrderBookSnapshot;
import com.tradingengine.dto.TradeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public DashboardBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastTrade(TradeEvent event) {
        messagingTemplate.convertAndSend("/topic/trades", event);
    }

    public void broadcastOrderBook(OrderBookSnapshot snapshot) {
        messagingTemplate.convertAndSend("/topic/orderbook/" + snapshot.getTicker(), snapshot);
    }
}