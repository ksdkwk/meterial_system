package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.springbootbackeng.entity.OperationLog;
import com.example.springbootbackeng.entity.OperationLogVO;
import com.example.springbootbackeng.mapper.OperationLogMapper;
import com.example.springbootbackeng.service.OperationLogService;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public List<OperationLogVO> listAll(String keyword, String operationType) {
        return operationLogMapper.selectAllWithDetails(keyword, operationType);
    }

    @Override
    public String create(OperationLog log) {
        if (log == null) return "请求参数不能为空";
        if (isBlank(log.getOperationModule())) return "操作模块不能为空";
        if (isBlank(log.getOperationType())) return "操作类型不能为空";

        String opType = log.getOperationType().trim().toUpperCase(Locale.ROOT);
        if (!("ADD".equals(opType) || "EDIT".equals(opType) || "DELETE".equals(opType) || "AUDIT".equals(opType))) {
            return "操作类型仅支持 ADD/EDIT/DELETE/AUDIT";
        }
        log.setOperationType(opType);

        if (isBlank(log.getLogId())) {
            log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        }

        return operationLogMapper.insert(log) > 0 ? null : "新增系统操作日志失败";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
