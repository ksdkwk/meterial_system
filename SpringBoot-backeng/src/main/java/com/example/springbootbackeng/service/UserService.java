package com.example.springbootbackeng.service;

import com.example.springbootbackeng.dto.LoginRequest;
import com.example.springbootbackeng.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 登录验证
     */
    User login(LoginRequest loginRequest);

    /**
     * 根据账号查询正常状态用户（找回密码场景）
     */
    User findActiveUserByUsername(String username);

    /**
     * 找回密码场景：按账号重置密码
     */
    boolean resetPasswordByUsername(String username, String newPassword);

    /**
     * 获取总用户数
     */
    long getTotalUserCount();

    /**
     * 获取用户列表（支持关键字/角色/状态筛选，隐藏密码）
     */
    java.util.List<User> getUserList(String keyword, Integer roleId, Integer status, String sortBy, String sortOrder);

    /**
     * 新增用户
     */
    String createUser(User user);

    /**
     * 修改用户
     */
    String updateUser(String userId, User user);

    /**
     * 删除用户
     */
    boolean deleteUser(String userId);

    /**
     * 批量删除用户
     */
    boolean batchDeleteUsers(java.util.List<String> userIds);

    /**
     * 根据 userId 判断是否管理人员（MANAGER）
     */
    boolean isManagerByUserId(String userId);

    /**
     * 根据角色编码判断是否管理人员
     */
    boolean isManagerRoleCode(String roleCode);

    /**
     * 根据 userId 判断是否仓库管理员（WAREHOUSE）
     */
    boolean isWarehouseByUserId(String userId);

    /**
     * 根据角色编码判断是否仓库管理员
     */
    boolean isWarehouseRoleCode(String roleCode);

    /**
     * 根据 userId 查询用户
     */
    User findByUserId(String userId);

    /**
     * 更新当前用户绑定邮箱
     */
    boolean updateUserMail(String userId, String userMail);

    /**
     * 更新当前用户绑定手机号
     */
    boolean updateUserPhone(String userId, String phone);
}