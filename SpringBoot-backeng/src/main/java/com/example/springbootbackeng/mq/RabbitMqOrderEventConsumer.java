package com.example.springbootbackeng.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mq", name = "provider", havingValue = "rabbit")
public class RabbitMqOrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqOrderEventConsumer.class);

    @RabbitListener(queues = MqConstants.RABBIT_ORDER_QUEUE)
    public void onOrderCreated(OrderCreatedMessage message) {
        // 预留：异步下单后置处理（库存预占、风控、通知等）
        if (message != null) {
            log.info("收到RabbitMQ下单消息: demandId={}, materialId={}, qty={}",
                    message.getDemandId(), message.getMaterialId(), message.getDemandQuantity());
        }
    }
}
