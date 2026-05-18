package com.example.springbootbackeng.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLoginCaptchaGuardService {

    private static final String PREFIX_FAIL_IP = "state:login:captcha:fail:ip:";
    private static final String PREFIX_FAIL_ACCOUNT = "state:login:captcha:fail:account:";
    private static final String PREFIX_LOCK_IP = "state:login:captcha:lock:ip:";
    private static final String PREFIX_LOCK_ACCOUNT = "state:login:captcha:lock:account:";

    private final StringRedisTemplate redisTemplate;

    public RedisLoginCaptchaGuardService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String ip, String account) {
        return hasLock(ipLockKey(ip)) || hasLock(accountLockKey(account));
    }

    public long getRemainLockSeconds(String ip, String account) {
        long ipRemain = remainSeconds(ipLockKey(ip));
        long accountRemain = remainSeconds(accountLockKey(account));
        return Math.max(ipRemain, accountRemain);
    }

    public boolean recordFailure(String ip,
                                 String account,
                                 int maxFailTimes,
                                 Duration failWindow,
                                 Duration lockDuration) {
        boolean ipLocked = increaseAndMaybeLock(ipFailKey(ip), ipLockKey(ip), maxFailTimes, failWindow, lockDuration);
        boolean accountLocked = increaseAndMaybeLock(accountFailKey(account), accountLockKey(account), maxFailTimes, failWindow, lockDuration);
        return ipLocked || accountLocked;
    }

    public void clearFailures(String ip, String account) {
        redisTemplate.delete(ipFailKey(ip));
        redisTemplate.delete(accountFailKey(account));
        redisTemplate.delete(ipLockKey(ip));
        redisTemplate.delete(accountLockKey(account));
    }

    private boolean increaseAndMaybeLock(String failKey,
                                         String lockKey,
                                         int maxFailTimes,
                                         Duration failWindow,
                                         Duration lockDuration) {
        if (hasLock(lockKey)) {
            return true;
        }

        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, failWindow);
        }

        if (count != null && count >= maxFailTimes) {
            redisTemplate.opsForValue().set(lockKey, "1", lockDuration);
            redisTemplate.delete(failKey);
            return true;
        }
        return false;
    }

    private boolean hasLock(String lockKey) {
        String value = redisTemplate.opsForValue().get(lockKey);
        return "1".equals(value);
    }

    private long remainSeconds(String lockKey) {
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return 0;
        }
        return ttl;
    }

    private String ipFailKey(String ip) {
        return PREFIX_FAIL_IP + normalize(ip);
    }

    private String accountFailKey(String account) {
        return PREFIX_FAIL_ACCOUNT + normalize(account);
    }

    private String ipLockKey(String ip) {
        return PREFIX_LOCK_IP + normalize(ip);
    }

    private String accountLockKey(String account) {
        return PREFIX_LOCK_ACCOUNT + normalize(account);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
