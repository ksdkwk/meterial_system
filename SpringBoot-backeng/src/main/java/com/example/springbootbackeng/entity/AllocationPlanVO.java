package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AllocationPlanVO {
    private String planId;
    private String demandId;
    private String materialId;
    private String materialName;
    private Integer allocQuantity;
    private String pickLocation;
    private LocalDateTime pickTime;
    private String allocatorId;
    private String allocatorName;
    private Integer confirmStatus;
    private LocalDateTime signTime;
    private String signUserId;
    private String signUserName;
    private String signPhotoUrl;
    private LocalDateTime createTime;
}
