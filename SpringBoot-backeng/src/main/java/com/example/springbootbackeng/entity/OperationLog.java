package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_operation_log")
public class OperationLog {

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;

    private String userId;
    private String operationModule;
    private String operationType;
    private String operationDesc;
    private String requestIp;
    private LocalDateTime operateTime;
}
