package com.example.springbootbackeng.mq;

import lombok.Data;

@Data
public class OperationLogMessage {
    private String userId;
    private String operationModule;
    private String operationType;
    private String operationDesc;
    private String requestIp;
}
