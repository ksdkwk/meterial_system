package com.example.springbootbackeng.mq;

public final class MqConstants {

    private MqConstants() {
    }

    public static final String RABBIT_EXCHANGE = "material.system.exchange";
    public static final String RABBIT_ORDER_QUEUE = "material.order.queue";
    public static final String RABBIT_OPERATION_LOG_QUEUE = "material.oplog.queue";
    public static final String RABBIT_ORDER_ROUTING_KEY = "material.order.created";
    public static final String RABBIT_OPERATION_LOG_ROUTING_KEY = "material.oplog.write";

    public static final String KAFKA_ORDER_TOPIC = "material-order-created";
    public static final String KAFKA_OPERATION_LOG_TOPIC = "material-oplog-write";
}
