package com.example.springbootbackeng.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.Vehicle;

@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
}
