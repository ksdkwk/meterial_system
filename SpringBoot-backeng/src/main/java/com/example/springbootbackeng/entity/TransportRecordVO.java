package com.example.springbootbackeng.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TransportRecordVO {
    private String recordId;
    private String planId;
    private String demandId;
    private String materialName;
    private String vehicleId;
    private String plateNumber;
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
