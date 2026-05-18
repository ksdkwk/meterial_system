package com.example.springbootbackeng.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_transport_record")
public class TransportRecord {

    @TableId(value = "record_id", type = IdType.INPUT)
    private String recordId;

    private String planId;
    private String vehicleId;
    private String driverName;
    private String driverPhone;
    private LocalDateTime departTime;
    private LocalDateTime expectArriveTime;
    private LocalDateTime actualArriveTime;
    private Integer transportStatus;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private LocalDateTime lastUpdateTime;
    private LocalDateTime createTime;
}
