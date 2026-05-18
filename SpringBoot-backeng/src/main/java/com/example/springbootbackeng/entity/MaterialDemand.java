package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_material_demand")
public class MaterialDemand {

    @TableId(value = "demand_id", type = IdType.INPUT)
    private String demandId;

    private String demandUserId;
    private String materialId;
    private Integer demandQuantity;
    private LocalDateTime useTime;
    private Integer demandStatus;
    private String rejectReason;
    private String sitePhotoUrl;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private String auditorId;
}
