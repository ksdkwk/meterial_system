package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MaterialDemandVO {
    private String demandId;
    private String demandUserId;
    private String demandUserName;
    private String demandUserAccount;
    private String materialId;
    private String materialName;
    private Integer demandQuantity;
    private LocalDateTime useTime;
    private Integer demandStatus;
    private String rejectReason;
    private String sitePhotoUrl;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private String auditorId;
    private String auditorName;
}
