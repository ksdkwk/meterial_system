package com.example.springbootbackeng.mq;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.springbootbackeng.entity.OperationLog;
import com.example.springbootbackeng.mapper.OperationLogMapper;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mq", name = "provider", havingValue = "kafka")
public class KafkaOperationLogConsumer {

    private final OperationLogMapper operationLogMapper;

    public KafkaOperationLogConsumer(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @KafkaListener(topics = MqConstants.KAFKA_OPERATION_LOG_TOPIC, groupId = "material-oplog-consumer")
    public void onOperationLog(OperationLogMessage message) {
        if (message == null) {
            return;
        }
        OperationLog log = new OperationLog();
        log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        log.setUserId(message.getUserId());
        log.setOperationModule(message.getOperationModule());
        log.setOperationType(normalizeType(message.getOperationType()));
        log.setOperationDesc(message.getOperationDesc());
        log.setRequestIp(message.getRequestIp());
        log.setOperateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private String normalizeType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "EDIT";
        }
        String type = value.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "ADD", "EDIT", "DELETE", "AUDIT" -> type;
            default -> "EDIT";
        };
    }
}
