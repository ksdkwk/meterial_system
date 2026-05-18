package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_password_update")
public class PasswordUpdate {

    @TableId(value = "record_id", type = IdType.INPUT)
    private String recordId;

    private String userId;
    private String username;
    private String realName;

    private LocalDateTime applyTime;
    private Integer updateStatus;
    private String failReason;

    private String osType;
    private String browserType;
    private String ipAddress;
    private String physicalLocation;

    private String verifyMethod;
    private String updateVerifyCode;
    private String verifyCode;

    private LocalDateTime completeTime;
    private String handlerId;

    private Integer isSuspicious;
    private Integer riskLevel;

    private String remark;
    private String extraInfo;

    private LocalDateTime createTime;
}
