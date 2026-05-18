package com.example.springbootbackeng.mq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopMqEventPublisher implements MqEventPublisher {
    @Override
    public void publishOrderCreated(OrderCreatedMessage message) {
        // mq未启用时，降级为无操作
    }

    @Override
    public void publishOperationLog(OperationLogMessage message) {
        // mq未启用时，降级为无操作
    }
}
