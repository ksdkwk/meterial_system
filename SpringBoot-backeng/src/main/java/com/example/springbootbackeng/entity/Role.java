package com.example.springbootbackeng.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色权限表（tb_role）
 */
@Data
@TableName("tb_role")
public class Role {
    @TableId(type = IdType.AUTO) // 自增主键（匹配你的role_id）
    private Integer roleId;         // 角色ID
    private String roleName;        // 角色名称（管理人员/施工人员/仓库管理员）
    private String roleCode;        // 角色编码（MANAGER/WORKER/WAREHOUSE）
    private String description;     // 角色描述
    private LocalDateTime createTime; // 创建时间
}