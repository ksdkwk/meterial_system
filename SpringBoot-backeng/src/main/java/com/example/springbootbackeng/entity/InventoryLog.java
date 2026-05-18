package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_inventory_log")
public class InventoryLog {

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;

    private String inventoryId;
    private String materialId;
    private Integer changeType;
    private Integer changeQuantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private String reason;
    @TableField("log_Change")
    private String logChange;
    private String operatorId;
    private LocalDateTime operateTime;
    private Integer logStatus;
}
