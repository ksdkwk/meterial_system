package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_allocation_plan")
public class AllocationPlan {

    @TableId(value = "plan_id", type = IdType.INPUT)
    private String planId;

    private String demandId;
    private Integer allocQuantity;
    private String pickLocation;
    private LocalDateTime pickTime;
    private String allocatorId;
    private Integer confirmStatus;
    private LocalDateTime signTime;
    private String signUserId;
    private String signPhotoUrl;
    private LocalDateTime createTime;
}
