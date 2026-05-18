package com.example.springbootbackeng.service.impl;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.springbootbackeng.service.PhoneCodeService;
import com.example.springbootbackeng.service.RedisRecoveryStoreService;

/**
 * 手机验证码服务实现（当前为开发环境模拟发送）
 */
@Service
public class PhoneCodeServiceImpl implements PhoneCodeService {

    private static final Logger log = LoggerFactory.getLogger(PhoneCodeServiceImpl.class);

    @Value("${recovery.code.expire-minutes:10}")
    private int expireMinutes;

    private final RedisRecoveryStoreService redisRecoveryStoreService;

    public PhoneCodeServiceImpl(RedisRecoveryStoreService redisRecoveryStoreService) {
        this.redisRecoveryStoreService = redisRecoveryStoreService;
    }

    @Override
    public String sendPhoneCode(String username, String phone) {
        String code = generateCode();
        redisRecoveryStoreService.savePhoneCode(username, code, Duration.ofMinutes(expireMinutes));

        // 生产环境请替换为真实短信网关发送逻辑
        log.info("[RecoveryPhoneCode] username={}, phone={}, code={}, expireMinutes={}",
                username, maskPhone(phone), code, expireMinutes);
        return code;
    }

    @Override
    public boolean verifyPhoneCode(String username, String code) {
        if (username == null || code == null) {
            return false;
        }
        return redisRecoveryStoreService.verifyAndConsumePhoneCode(username, code);
    }

    private String generateCode() {
        int n = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(n);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.trim().length() < 7) {
            return "******";
        }
        String p = phone.trim();
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

}
