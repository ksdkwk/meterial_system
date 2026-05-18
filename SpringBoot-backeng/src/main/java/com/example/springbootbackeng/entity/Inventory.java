package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_inventory")
public class Inventory {

    @TableId(value = "inventory_id", type = IdType.INPUT)
    private String inventoryId;

    private String materialId;
    private String storageLocation;
    private Integer stockQuantity;
    private LocalDateTime lastCheckTime;
    private String lastCheckerId;
    private LocalDateTime updateTime;
}
