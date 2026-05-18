package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 系统用户信息表（tb_user）
 */
@Data
@TableName("tb_user")
public class User {
    @TableId(type = IdType.INPUT) // UUID手动输入（匹配你的user_id）
    private String userId;         // 用户唯一ID（UUID）
    private String username;        // 登录账号（如admin）
    private String password;        // 登录密码（明文/加密）
    private Integer roleId;         // 关联角色ID
    private String realName;        // 真实姓名
    private String phone;           // 手机号
    @TableField("user_mail")
    private String userMail;        // 邮箱
    private String teamId;          // 所属班组ID
    private String openid;          // 微信OpenID
    private String avatarUrl;       // 头像URL
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer status;         // 账号状态：0-禁用，1-正常
    private String rolePermission;  // 角色权限

    // 扩展字段：关联角色信息（非数据库字段）
    @TableField(exist = false)
    private Role role;
}