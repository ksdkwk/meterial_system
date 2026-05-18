package com.example.springbootbackeng.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mq", name = "provider", havingValue = "kafka")
public class KafkaOrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventConsumer.class);

    @KafkaListener(topics = MqConstants.KAFKA_ORDER_TOPIC, groupId = "material-order-consumer")
    public void onOrderCreated(OrderCreatedMessage message) {
        if (message != null) {
            log.info("收到Kafka下单消息: demandId={}, materialId={}, qty={}",
                    message.getDemandId(), message.getMaterialId(), message.getDemandQuantity());
        }
    }
}
