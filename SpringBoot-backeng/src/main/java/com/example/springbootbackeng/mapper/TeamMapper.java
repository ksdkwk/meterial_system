package com.example.springbootbackeng.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.Team;

@Mapper
public interface TeamMapper extends BaseMapper<Team> {
}
