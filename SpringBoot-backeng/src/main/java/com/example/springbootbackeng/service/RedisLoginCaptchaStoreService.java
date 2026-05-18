package com.example.springbootbackeng.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisLoginCaptchaStoreService {

    private static final String PREFIX_CAPTCHA = "state:login:captcha:";

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisLoginCaptchaStoreService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveCaptcha(String captchaId, String captchaCode, Duration ttl) {
        redisTemplate.opsForValue().set(captchaKey(captchaId), normalizeCode(captchaCode), ttl);
    }

    public boolean verifyAndConsumeCaptcha(String captchaId, String inputCode) {
        Long ok = redisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                List.of(captchaKey(captchaId)),
                normalizeCode(inputCode)
        );
        return ok != null && ok > 0;
    }

    private String captchaKey(String captchaId) {
        return PREFIX_CAPTCHA + normalizeId(captchaId);
    }

    private String normalizeId(String captchaId) {
        return captchaId == null ? "" : captchaId.trim();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
