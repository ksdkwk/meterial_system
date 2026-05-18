package com.example.springbootbackeng.cache;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheBloomService {

    private static final String MATERIAL_ID_BLOOM_KEY = "cache:bloom:material:id";
    private static final int[] HASH_SEEDS = {17, 31, 131};

    private final StringRedisTemplate redisTemplate;

    @Value("${app.cache.bloom.enabled:true}")
    private boolean bloomEnabled;

    @Value("${app.cache.bloom.material-id-size:1048576}")
    private int materialBloomSize;

    public CacheBloomService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean enabled() {
        return bloomEnabled;
    }

    public void addMaterialId(String materialId) {
        if (!bloomEnabled || materialId == null || materialId.trim().isEmpty()) {
            return;
        }
        for (int seed : HASH_SEEDS) {
            redisTemplate.opsForValue().setBit(MATERIAL_ID_BLOOM_KEY, index(materialId, seed), true);
        }
    }

    public boolean mightContainMaterialId(String materialId) {
        if (!bloomEnabled || materialId == null || materialId.trim().isEmpty()) {
            return true;
        }
        for (int seed : HASH_SEEDS) {
            Boolean bit = redisTemplate.opsForValue().getBit(MATERIAL_ID_BLOOM_KEY, index(materialId, seed));
            if (!Boolean.TRUE.equals(bit)) {
                return false;
            }
        }
        return true;
    }

    private long index(String value, int seed) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        long hash = 2166136261L;
        for (byte b : bytes) {
            hash ^= b;
            hash *= 16777619;
            hash += seed;
        }
        long positive = hash & Long.MAX_VALUE;
        int size = Math.max(1024, materialBloomSize);
        return positive % size;
    }
}
