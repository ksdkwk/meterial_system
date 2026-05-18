package com.example.springbootbackeng.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRecoveryStoreService {

    private static final String PREFIX_EMAIL_CODE = "state:recovery:email-code:";
    private static final String PREFIX_PHONE_CODE = "state:recovery:phone-code:";
    private static final String PREFIX_VERIFIED = "state:recovery:verified:";

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisRecoveryStoreService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveEmailCode(String username, String code, Duration ttl) {
        redisTemplate.opsForValue().set(emailCodeKey(username), code, ttl);
    }

    public boolean verifyAndConsumeEmailCode(String username, String code) {
        Long ok = redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(emailCodeKey(username)), safe(code));
        return ok != null && ok > 0;
    }

    public void savePhoneCode(String username, String code, Duration ttl) {
        redisTemplate.opsForValue().set(phoneCodeKey(username), code, ttl);
    }

    public boolean verifyAndConsumePhoneCode(String username, String code) {
        Long ok = redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(phoneCodeKey(username)), safe(code));
        return ok != null && ok > 0;
    }

    public void markVerified(String username, Duration ttl) {
        redisTemplate.opsForValue().set(verifiedKey(username), "1", ttl);
    }

    public boolean isVerified(String username) {
        String value = redisTemplate.opsForValue().get(verifiedKey(username));
        return "1".equals(value);
    }

    public void clearVerified(String username) {
        redisTemplate.delete(verifiedKey(username));
    }

    private String emailCodeKey(String username) {
        return PREFIX_EMAIL_CODE + normalize(username);
    }

    private String phoneCodeKey(String username) {
        return PREFIX_PHONE_CODE + normalize(username);
    }

    private String verifiedKey(String username) {
        return PREFIX_VERIFIED + normalize(username);
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
