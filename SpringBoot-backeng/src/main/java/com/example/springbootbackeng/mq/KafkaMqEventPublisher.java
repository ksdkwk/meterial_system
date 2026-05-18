package com.example.springbootbackeng.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mq", name = "provider", havingValue = "kafka")
public class KafkaMqEventPublisher implements MqEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMqEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMqEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishOrderCreated(OrderCreatedMessage message) {
        try {
            kafkaTemplate.send(MqConstants.KAFKA_ORDER_TOPIC, message == null ? null : message.getDemandId(), message);
        } catch (RuntimeException ex) {
            log.warn("发送Kafka订单消息失败，已降级忽略。demandId={}", message == null ? null : message.getDemandId(), ex);
        }
    }

    @Override
    public void publishOperationLog(OperationLogMessage message) {
        try {
            kafkaTemplate.send(MqConstants.KAFKA_OPERATION_LOG_TOPIC, message == null ? null : message.getUserId(), message);
        } catch (RuntimeException ex) {
            log.warn("发送Kafka操作日志消息失败，已降级忽略。module={}", message == null ? null : message.getOperationModule(), ex);
        }
    }
}
