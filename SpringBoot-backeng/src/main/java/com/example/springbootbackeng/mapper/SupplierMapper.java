package com.example.springbootbackeng.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.Supplier;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
