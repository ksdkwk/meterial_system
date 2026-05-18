package com.example.springbootbackeng.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootbackeng.entity.User;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据账号+角色ID+账号状态查询用户
     */
    User selectByUsernameAndRoleId(
            @Param("username") String username,
            @Param("roleId") Integer roleId,
            @Param("status") Integer status);

        /**
         * 根据账号+状态查询用户（找回密码场景）
         */
        User selectByUsernameAndStatus(
            @Param("username") String username,
            @Param("status") Integer status);
}