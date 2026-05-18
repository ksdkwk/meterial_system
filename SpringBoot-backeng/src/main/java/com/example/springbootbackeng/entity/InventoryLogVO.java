package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InventoryLogVO {
    private String logId;
    private String inventoryId;
    private String materialId;
    private String materialName;
    private Integer changeType;
    private Integer changeQuantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private String reason;
    private String logChange;
    private String operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
    private Integer logStatus;
}
