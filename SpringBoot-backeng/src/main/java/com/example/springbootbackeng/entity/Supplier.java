package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_supplier")
public class Supplier {

    @TableId(value = "supplier_id", type = IdType.INPUT)
    private String supplierId;

    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private LocalDateTime createTime;
    private Integer status;
}
