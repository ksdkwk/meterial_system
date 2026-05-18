package com.example.springbootbackeng.service;

/**
 * 邮箱验证码服务
 */
public interface MailCodeService {

    /**
     * 发送6位验证码到邮箱
     */
    String sendEmailCode(String username, String email);

    /**
     * 校验邮箱验证码
     */
    boolean verifyEmailCode(String username, String code);
}
