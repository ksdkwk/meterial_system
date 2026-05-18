package com.example.springbootbackeng.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.springbootbackeng.dto.LoginRequest;
import com.example.springbootbackeng.entity.Role;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.mapper.RoleMapper;
import com.example.springbootbackeng.mapper.UserMapper;
import com.example.springbootbackeng.service.UserService;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(LoginRequest loginRequest) {
        // 1. 校验角色编码
        String roleCode = loginRequest.getRoleCode();
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return null;
        }

        // 2. 根据角色编码查询角色（匹配数据库的MANAGER/WORKER/WAREHOUSE）
        Role role = roleMapper.selectByRoleCode(roleCode.toUpperCase());
        if (role == null) {
            return null; // 角色不存在
        }

        // 3. 查询用户（账号+角色ID+状态=1（正常））
        User user = userMapper.selectByUsernameAndRoleId(
                loginRequest.getUsername(),
                role.getRoleId(),
                1 // 仅查询正常状态的账号
        );

        if (user == null) {
            return null;
        }

        // 4. 验证密码（兼容历史明文密码）
        String rawPassword = loginRequest.getPassword();
        String dbPassword = user.getPassword();
        if (rawPassword == null || dbPassword == null || dbPassword.trim().isEmpty()) {
            return null;
        }

        boolean matched;
        boolean needMigrate = false;

        if (isBcryptHash(dbPassword)) {
            matched = passwordEncoder.matches(rawPassword, dbPassword);
        } else {
            matched = dbPassword.equals(rawPassword);
            needMigrate = matched;
        }

        if (matched) {
            if (needMigrate) {
                migratePlainPasswordToBcrypt(user.getUserId(), rawPassword);
            }
            user.setRole(role); // 关联角色信息
            user.setPassword(null); // 隐藏密码，避免返回
            return user;
        }

        return null; // 账号/密码/角色不匹配
    }

    @Override
    public User findActiveUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectByUsernameAndStatus(username.trim(), 1);
    }

    @Override
    public boolean resetPasswordByUsername(String username, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        String encoded = encodeIfNecessary(newPassword.trim());
        wrapper.eq(User::getUsername, username.trim())
                .eq(User::getStatus, 1)
            .set(User::getPassword, encoded);
        return userMapper.update(null, wrapper) > 0;
    }

    @Override
    public long getTotalUserCount() {
        return userMapper.selectCount(null);
    }

    @Override
    public java.util.List<User> getUserList(String keyword, Integer roleId, Integer status, String sortBy, String sortOrder) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }

        wrapper.eq(roleId != null, User::getRoleId, roleId)
                .eq(status != null, User::getStatus, status);

        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        String safeSortBy = sortBy == null ? "createTime" : sortBy;
        switch (safeSortBy) {
            case "username":
                wrapper.orderBy(true, asc, User::getUsername);
                break;
            case "realName":
                wrapper.orderBy(true, asc, User::getRealName);
                break;
            case "roleId":
                wrapper.orderBy(true, asc, User::getRoleId);
                break;
            case "status":
                wrapper.orderBy(true, asc, User::getStatus);
                break;
            case "updateTime":
                wrapper.orderBy(true, asc, User::getUpdateTime);
                break;
            case "createTime":
            default:
                wrapper.orderBy(true, asc, User::getCreateTime);
                break;
        }

        java.util.List<User> users = userMapper.selectList(wrapper);
        users.forEach(u -> {
            u.setPassword(null); // 隐藏密码
            // 关联角色信息
            if (u.getRoleId() != null) {
                Role role = roleMapper.selectById(u.getRoleId());
                u.setRole(role);
            }
        });
        return users;
    }

    @Override
    public String createUser(User user) {
        String validateMsg = validateUser(user, false);
        if (validateMsg != null) {
            return validateMsg;
        }

        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            return "账号已存在，请更换账号";
        }

        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            user.setUserId(java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        user.setPassword(encodeIfNecessary(user.getPassword()));

        setRolePermissionByRoleId(user);
        return userMapper.insert(user) > 0 ? null : "新增用户失败";
    }

    @Override
    public String updateUser(String userId, User user) {
        User old = userMapper.selectById(userId);
        if (old == null) {
            return "用户不存在";
        }

        user.setUserId(userId);

        String validateMsg = validateUser(user, true);
        if (validateMsg != null) {
            return validateMsg;
        }

        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername())
                        .ne(User::getUserId, userId));
        if (exists != null && exists > 0) {
            return "账号已存在，请更换账号";
        }

        // 密码为空时不更新密码
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(old.getPassword());
        } else {
            user.setPassword(encodeIfNecessary(user.getPassword()));
        }

        if (user.getRoleId() == null) {
            user.setRoleId(old.getRoleId());
        }
        if (user.getRealName() == null) {
            user.setRealName(old.getRealName());
        }
        if (user.getPhone() == null) {
            user.setPhone(old.getPhone());
        }
        if (user.getStatus() == null) {
            user.setStatus(old.getStatus());
        }
        if (user.getTeamId() == null) {
            user.setTeamId(old.getTeamId());
        }
        if (user.getOpenid() == null) {
            user.setOpenid(old.getOpenid());
        }
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(old.getAvatarUrl());
        }

        setRolePermissionByRoleId(user);

        return userMapper.updateById(user) > 0 ? null : "修改用户失败";
    }

    @Override
    public boolean deleteUser(String userId) {
        return userMapper.deleteById(userId) > 0;
    }

    @Override
    public boolean batchDeleteUsers(java.util.List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        return userMapper.deleteByIds(userIds) > 0;
    }

    private String validateUser(User user, boolean isEdit) {
        if (!isEdit && (user.getUsername() == null || user.getUsername().trim().isEmpty())) {
            return "账号不能为空";
        }

        if (!isEdit && (user.getPassword() == null || user.getPassword().trim().isEmpty())) {
            return "密码不能为空";
        }

        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!user.getPhone().matches("^1\\d{10}$")) {
                return "手机号格式不正确";
            }
        }

        if (user.getRoleId() != null && (user.getRoleId() < 1 || user.getRoleId() > 3)) {
            return "角色ID不合法";
        }

        return null;
    }

    private void setRolePermissionByRoleId(User user) {
        if (user.getRoleId() == null) {
            return;
        }
        switch (user.getRoleId()) {
            case 1 -> user.setRolePermission("user_admin");
            case 2 -> user.setRolePermission("user_worker");
            case 3 -> user.setRolePermission("user_warehouse");
            default -> {
            }
        }
    }

    private String encodeIfNecessary(String password) {
        if (password == null || password.trim().isEmpty()) {
            return password;
        }
        String p = password.trim();
        if (isBcryptHash(p)) {
            return p;
        }
        return passwordEncoder.encode(p);
    }

    private boolean isBcryptHash(String password) {
        if (password == null) {
            return false;
        }
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private void migratePlainPasswordToBcrypt(String userId, String rawPassword) {
        if (userId == null || userId.trim().isEmpty() || rawPassword == null || rawPassword.trim().isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId.trim())
                .set(User::getPassword, passwordEncoder.encode(rawPassword.trim()));
        userMapper.update(null, wrapper);
    }

    @Override
    public boolean isManagerByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            return false;
        }

        // 优先按 role_id -> tb_role.role_code 判断
        if (dbUser.getRoleId() != null) {
            Role role = roleMapper.selectById(dbUser.getRoleId());
            if (role != null && isManagerRoleCode(role.getRoleCode())) {
                return true;
            }
        }

        // 兼容项目中的 rolePermission 字段
        try {
            String rp = dbUser.getRolePermission();
            return isManagerRoleCode(rp);
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public boolean isWarehouseByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            return false;
        }

        if (dbUser.getRoleId() != null) {
            Role role = roleMapper.selectById(dbUser.getRoleId());
            if (role != null && isWarehouseRoleCode(role.getRoleCode())) {
                return true;
            }
        }

        try {
            String rp = dbUser.getRolePermission();
            return isWarehouseRoleCode(rp);
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public boolean isManagerRoleCode(String roleCode) {
        if (roleCode == null) return false;
        String rc = roleCode.trim().toUpperCase();
        return "MANAGER".equals(rc) || "ADMIN".equals(rc) || "SYSTEM_ADMIN".equals(rc) || "SUPER_ADMIN".equals(rc);
    }

    @Override
    public boolean isWarehouseRoleCode(String roleCode) {
        if (roleCode == null) return false;
        String rc = roleCode.trim().toUpperCase();
        return "WAREHOUSE".equals(rc) || "WAREHOUSE_ADMIN".equals(rc) || "USER_WAREHOUSE".equals(rc);
    }

    @Override
    public User findByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        User user = userMapper.selectById(userId.trim());
        if (user != null) {
            user.setPassword(null);
            if (user.getRoleId() != null) {
                Role role = roleMapper.selectById(user.getRoleId());
                user.setRole(role);
            }
        }
        return user;
    }

    @Override
    public boolean updateUserMail(String userId, String userMail) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId.trim())
                .eq(User::getStatus, 1)
                .set(User::getUserMail, userMail == null ? null : userMail.trim());
        return userMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean updateUserPhone(String userId, String phone) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId.trim())
                .eq(User::getStatus, 1)
                .set(User::getPhone, phone == null ? null : phone.trim());
        return userMapper.update(null, wrapper) > 0;
    }
}