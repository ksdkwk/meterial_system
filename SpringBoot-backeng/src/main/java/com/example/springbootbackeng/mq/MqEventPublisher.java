package com.example.springbootbackeng.mq;

public interface MqEventPublisher {

    void publishOrderCreated(OrderCreatedMessage message);

    void publishOperationLog(OperationLogMessage message);
}
