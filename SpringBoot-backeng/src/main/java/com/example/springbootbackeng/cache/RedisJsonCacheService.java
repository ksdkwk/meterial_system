package com.example.springbootbackeng.cache;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Component
public class RedisJsonCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonCacheService.class);
    private static final String NULL_MARKER = "__NULL__";
    private static final String LOGICAL_EXPIRE_AT_FIELD = "expireAt";
    private static final String LOGICAL_DATA_FIELD = "data";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.ttl-jitter-max-seconds:120}")
    private long ttlJitterMaxSeconds;

    @Value("${app.cache.null-ttl-jitter-max-seconds:30}")
    private long nullTtlJitterMaxSeconds;

    public RedisJsonCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    public <T> T get(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank() || NULL_MARKER.equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException | RuntimeException ex) {
            log.warn("缓存反序列化失败，已忽略该缓存。key={}", key, ex);
            redisTemplate.delete(key);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank() || NULL_MARKER.equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException | RuntimeException ex) {
            log.warn("缓存反序列化失败，已忽略该缓存。key={}", key, ex);
            redisTemplate.delete(key);
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, withJitter(ttl, ttlJitterMaxSeconds));
        } catch (JsonProcessingException ex) {
            log.warn("缓存序列化失败，已跳过写缓存。key={}", key, ex);
        }
    }

    public void setNull(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, NULL_MARKER, withJitter(ttl, nullTtlJitterMaxSeconds));
    }

    public boolean isNullMarker(String key) {
        String raw = redisTemplate.opsForValue().get(key);
        return NULL_MARKER.equals(raw);
    }

    public boolean tryLock(String lockKey, String token, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, token, ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String lockKey, String token) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
    }

    public void setWithLogicalExpire(String key, Object value, Duration logicalExpire, Duration redisTtl) {
        if (value == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put(LOGICAL_EXPIRE_AT_FIELD, LocalDateTime.now().plus(logicalExpire));
        payload.put(LOGICAL_DATA_FIELD, value);
        set(key, payload, redisTtl);
    }

    public <T> LogicalCacheValue<T> getLogical(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank() || NULL_MARKER.equals(json)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has(LOGICAL_EXPIRE_AT_FIELD) && root.has(LOGICAL_DATA_FIELD)) {
                LocalDateTime expireAt = objectMapper.convertValue(root.get(LOGICAL_EXPIRE_AT_FIELD), LocalDateTime.class);
                T data = objectMapper.convertValue(root.get(LOGICAL_DATA_FIELD), clazz);
                return new LogicalCacheValue<>(data, expireAt != null && expireAt.isBefore(LocalDateTime.now()));
            }
            T data = objectMapper.readValue(json, clazz);
            return new LogicalCacheValue<>(data, false);
        } catch (IOException | RuntimeException ex) {
            log.warn("逻辑过期缓存反序列化失败，已忽略该缓存。key={}", key, ex);
            redisTemplate.delete(key);
            return null;
        }
    }

    public <T> LogicalCacheValue<T> getLogical(String key, TypeReference<T> typeReference) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank() || NULL_MARKER.equals(json)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);
            if (root.has(LOGICAL_EXPIRE_AT_FIELD) && root.has(LOGICAL_DATA_FIELD)) {
                LocalDateTime expireAt = objectMapper.convertValue(root.get(LOGICAL_EXPIRE_AT_FIELD), LocalDateTime.class);
                T data = objectMapper.convertValue(root.get(LOGICAL_DATA_FIELD), javaType);
                return new LogicalCacheValue<>(data, expireAt != null && expireAt.isBefore(LocalDateTime.now()));
            }
            T data = objectMapper.readValue(json, typeReference);
            return new LogicalCacheValue<>(data, false);
        } catch (IOException | RuntimeException ex) {
            log.warn("逻辑过期缓存反序列化失败，已忽略该缓存。key={}", key, ex);
            redisTemplate.delete(key);
            return null;
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public static final class LogicalCacheValue<T> {
        private final T data;
        private final boolean expired;

        public LogicalCacheValue(T data, boolean expired) {
            this.data = data;
            this.expired = expired;
        }

        public T getData() {
            return data;
        }

        public boolean isExpired() {
            return expired;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LogicalCacheValue<?> that = (LogicalCacheValue<?>) o;
            return expired == that.expired && Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, expired);
        }
    }

    private Duration withJitter(Duration ttl, long maxJitterSeconds) {
        if (ttl == null || ttl.isNegative() || ttl.isZero() || maxJitterSeconds <= 0) {
            return ttl;
        }
        long jitter = ThreadLocalRandom.current().nextLong(maxJitterSeconds + 1);
        return ttl.plusSeconds(jitter);
    }
}
