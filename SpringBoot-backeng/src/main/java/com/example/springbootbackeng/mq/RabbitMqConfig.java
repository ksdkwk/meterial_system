package com.example.springbootbackeng.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(prefix = "app.mq", name = "provider", havingValue = "rabbit")
public class RabbitMqConfig {

    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange(MqConstants.RABBIT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue(MqConstants.RABBIT_ORDER_QUEUE, true);
    }

    @Bean
    public Queue operationLogQueue() {
        return new Queue(MqConstants.RABBIT_OPERATION_LOG_QUEUE, true);
    }

    @Bean
    public Binding orderBinding(DirectExchange businessExchange, Queue orderQueue) {
        return BindingBuilder.bind(orderQueue).to(businessExchange).with(MqConstants.RABBIT_ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding operationLogBinding(DirectExchange businessExchange, Queue operationLogQueue) {
        return BindingBuilder.bind(operationLogQueue).to(businessExchange).with(MqConstants.RABBIT_OPERATION_LOG_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
