package com.example.springbootbackeng.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_material")
public class Material {

    @TableId(value = "material_id", type = IdType.INPUT)
    private String materialId;

    private String materialName;
    private String specModel;
    private String materialType;
    private String unit;
    private BigDecimal unitPrice;
    private String supplierId;
    private String storageLocation;
    private Integer minThreshold;
    private Integer maxThreshold;
    private String imageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;
}