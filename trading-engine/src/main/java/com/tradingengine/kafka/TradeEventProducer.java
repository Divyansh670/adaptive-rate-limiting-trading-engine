package com.tradingengine.kafka;

import com.tradingengine.dto.TradeEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TradeEventProducer {

    public static final String TOPIC = "trade-events";

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;

    public TradeEventProducer(KafkaTemplate<String, TradeEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TradeEvent event) {
        kafkaTemplate.send(TOPIC, event.getTicker(), event);
    }
}