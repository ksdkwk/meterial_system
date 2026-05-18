package com.example.springbootbackeng.service;

/**
 * 手机验证码服务
 */
public interface PhoneCodeService {

    /**
     * 发送6位验证码到手机号
     */
    String sendPhoneCode(String username, String phone);

    /**
     * 校验手机验证码
     */
    boolean verifyPhoneCode(String username, String code);
}
