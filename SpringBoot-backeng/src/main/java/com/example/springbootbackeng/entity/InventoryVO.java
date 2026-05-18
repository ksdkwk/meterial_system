package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InventoryVO {
    private String inventoryId;
    private String materialId;
    private String materialName;
    private String storageLocation;
    private Integer stockQuantity;
    private LocalDateTime lastCheckTime;
    private String lastCheckerId;
    private String lastCheckerName;
    private LocalDateTime updateTime;
}
