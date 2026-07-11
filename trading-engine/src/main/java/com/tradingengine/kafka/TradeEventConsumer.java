package com.tradingengine.kafka;

import com.tradingengine.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TradeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    @KafkaListener(topics = TradeEventProducer.TOPIC, groupId = "trade-audit-group")
    public void consume(TradeEvent event) {
        log.info("Consumed trade event: tradeId={}, ticker={}, price={}, qty={}, buyOrder={}, sellOrder={}",
                event.getTradeId(), event.getTicker(), event.getPrice(), event.getQuantity(),
                event.getBuyOrderId(), event.getSellOrderId());
    }
}