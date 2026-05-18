package com.example.springbootbackeng.ratelimit;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisTokenBucketRateLimitService {

    private static final String PREFIX_TOKENS = "state:ratelimit:tokens:";
    private static final String PREFIX_TS = "state:ratelimit:ts:";

    private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
            "local tokens_key = KEYS[1] "
                    + "local ts_key = KEYS[2] "
                    + "local rate = tonumber(ARGV[1]) "
                    + "local capacity = tonumber(ARGV[2]) "
                    + "local now_ms = tonumber(ARGV[3]) "
                    + "local requested = tonumber(ARGV[4]) "
                    + "local ttl_ms = tonumber(ARGV[5]) "
                    + "local last_tokens = tonumber(redis.call('get', tokens_key)) "
                    + "if last_tokens == nil then last_tokens = capacity end "
                    + "local last_ts = tonumber(redis.call('get', ts_key)) "
                    + "if last_ts == nil then last_ts = now_ms end "
                    + "local delta = math.max(0, now_ms - last_ts) "
                    + "local filled = math.min(capacity, last_tokens + (delta * rate / 1000.0)) "
                    + "local allowed = 0 "
                    + "local new_tokens = filled "
                    + "if filled >= requested then "
                    + "  allowed = 1 "
                    + "  new_tokens = filled - requested "
                    + "end "
                    + "redis.call('psetex', tokens_key, ttl_ms, new_tokens) "
                    + "redis.call('psetex', ts_key, ttl_ms, now_ms) "
                    + "return {allowed, new_tokens}",
            List.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBucketRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param key 业务唯一限流键
     * @param permitsPerSecond 每秒补充令牌数
     * @param burstCapacity 桶容量（突发量）
     */
    public boolean allow(String key, double permitsPerSecond, int burstCapacity) {
        String normalized = normalize(key);
        String tokensKey = PREFIX_TOKENS + normalized;
        String tsKey = PREFIX_TS + normalized;

        long nowMs = System.currentTimeMillis();
        long ttlMs = Math.max(1_000L, Duration.ofSeconds(Math.max(2, burstCapacity)).toMillis());

        List result = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(tokensKey, tsKey),
                String.valueOf(Math.max(0.0001D, permitsPerSecond)),
                String.valueOf(Math.max(1, burstCapacity)),
                String.valueOf(nowMs),
                "1",
                String.valueOf(ttlMs)
        );

        if (result == null || result.isEmpty()) {
            return true;
        }
        Object allowed = result.get(0);
        return "1".equals(String.valueOf(allowed));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
