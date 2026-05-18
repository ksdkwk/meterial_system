package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OperationLogVO {
    private String logId;
    private String userId;
    private String userName;
    private String operationModule;
    private String operationType;
    private String operationDesc;
    private String requestIp;
    private LocalDateTime operateTime;
}
