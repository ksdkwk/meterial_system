package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_vehicle")
public class Vehicle {

    @TableId(value = "vehicle_id", type = IdType.INPUT)
    private String vehicleId;

    private String plateNumber;
    private String vehicleType;
    private String driverName;
    private String driverPhone;
    private Integer status;
    private LocalDateTime createTime;
}
