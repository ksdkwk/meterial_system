package com.example.springbootbackeng.service;

import com.example.springbootbackeng.entity.User;

public interface PasswordUpdateService {

    /**
     * 发送验证码后创建密码找回记录（状态：处理中）
     */
    void createRecoveryRecord(User user,
                              String verifyMethod,
                              String sentCode,
                              String ipAddress,
                              String userAgent);

    /**
     * 验证码校验成功后更新最近一条处理中记录为成功
     */
    void markVerifySuccess(String username,
                           String verifyMethod,
                           String inputCode);

    /**
     * 验证码校验失败后更新最近一条处理中记录为失败
     */
    void markVerifyFailed(String username,
                          String verifyMethod,
                          String inputCode,
                          String failReason);
}
