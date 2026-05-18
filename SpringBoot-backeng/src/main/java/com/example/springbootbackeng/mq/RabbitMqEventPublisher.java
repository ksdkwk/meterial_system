package com.example.springbootbackeng.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mq", name = "provider", havingValue = "rabbit")
public class RabbitMqEventPublisher implements MqEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishOrderCreated(OrderCreatedMessage message) {
        try {
            rabbitTemplate.convertAndSend(MqConstants.RABBIT_EXCHANGE, MqConstants.RABBIT_ORDER_ROUTING_KEY, message);
        } catch (RuntimeException ex) {
            log.warn("发送RabbitMQ订单消息失败，已降级忽略。demandId={}", message == null ? null : message.getDemandId(), ex);
        }
    }

    @Override
    public void publishOperationLog(OperationLogMessage message) {
        try {
            rabbitTemplate.convertAndSend(MqConstants.RABBIT_EXCHANGE, MqConstants.RABBIT_OPERATION_LOG_ROUTING_KEY, message);
        } catch (RuntimeException ex) {
            log.warn("发送RabbitMQ操作日志消息失败，已降级忽略。module={}", message == null ? null : message.getOperationModule(), ex);
        }
    }
}
