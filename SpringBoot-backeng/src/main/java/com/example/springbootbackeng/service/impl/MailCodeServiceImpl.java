package com.example.springbootbackeng.service.impl;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.springbootbackeng.service.MailCodeService;
import com.example.springbootbackeng.service.RedisRecoveryStoreService;

/**
 * 邮箱验证码服务实现
 */
@Service
public class MailCodeServiceImpl implements MailCodeService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    @Value("${recovery.code.expire-minutes:10}")
    private int expireMinutes;

    @Autowired
    private RedisRecoveryStoreService redisRecoveryStoreService;

    @Override
    public String sendEmailCode(String username, String email) {
        String code = generateCode();
        redisRecoveryStoreService.saveEmailCode(username, code, Duration.ofMinutes(expireMinutes));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromMail);
        message.setTo(email);
        message.setSubject("【物料库服务系统】找回密码验证码");
        message.setText("你的验证码为：" + code + "，有效期 " + expireMinutes + " 分钟。若非本人操作，请忽略此邮件。");
        mailSender.send(message);
        return code;
    }

    @Override
    public boolean verifyEmailCode(String username, String code) {
        if (username == null || code == null) {
            return false;
        }
        return redisRecoveryStoreService.verifyAndConsumeEmailCode(username, code);
    }

    private String generateCode() {
        int n = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(n);
    }

}
