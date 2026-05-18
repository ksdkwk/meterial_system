package com.example.springbootbackeng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号长度不能超过64")
    private String username;    // 登录账号

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需在6-128位")
    private String password;    // 登录密码

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "MANAGER|WORKER|WAREHOUSE", message = "角色编码不合法")
    private String roleCode;    // 角色编码（MANAGER/WORKER/WAREHOUSE）

    @NotBlank(message = "验证码ID不能为空")
    @Size(max = 64, message = "验证码ID不合法")
    private String captchaId;   // 验证码ID

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "[A-Za-z0-9]{4}", message = "验证码格式不合法")
    private String captchaCode; // 用户输入验证码

    private Boolean rememberMe; // 是否记住我
}