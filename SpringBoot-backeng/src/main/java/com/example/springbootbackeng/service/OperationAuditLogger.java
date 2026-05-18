package com.example.springbootbackeng.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.springbootbackeng.entity.OperationLog;
import com.example.springbootbackeng.mapper.OperationLogMapper;

@Component
public class OperationAuditLogger {

    private final OperationLogMapper operationLogMapper;

    public OperationAuditLogger(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void log(String userId,
                    String operationModule,
                    String operationType,
                    String operationDesc,
                    String requestIp) {
        if (isBlank(operationModule) || isBlank(operationType)) {
            return;
        }

        OperationLog log = new OperationLog();
        log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        log.setUserId(isBlank(userId) ? null : userId.trim());
        log.setOperationModule(limit(operationModule, 50));
        log.setOperationType(normalizeType(operationType));
        log.setOperationDesc(limit(operationDesc, 255));
        log.setRequestIp(limit(requestIp, 50));
        log.setOperateTime(LocalDateTime.now());

        try {
            operationLogMapper.insert(log);
        } catch (RuntimeException ignored) {
            // 审计日志写入失败不影响主流程
        }
    }

    private String normalizeType(String opType) {
        String t = opType == null ? "" : opType.trim().toUpperCase(Locale.ROOT);
        if ("ADD".equals(t) || "EDIT".equals(t) || "DELETE".equals(t) || "AUDIT".equals(t)) {
            return t;
        }
        return "EDIT";
    }

    private String limit(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
