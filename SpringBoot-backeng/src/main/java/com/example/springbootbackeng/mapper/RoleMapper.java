package com.example.springbootbackeng.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    /**
     * 根据角色编码查询角色信息
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);
}