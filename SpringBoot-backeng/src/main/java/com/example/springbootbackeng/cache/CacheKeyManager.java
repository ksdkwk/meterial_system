package com.example.springbootbackeng.cache;

import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class CacheKeyManager {

    public static final Duration MATERIAL_DETAIL_TTL = Duration.ofMinutes(30);
    public static final Duration MATERIAL_LIST_TTL = Duration.ofMinutes(10);
    public static final Duration INVENTORY_LIST_TTL = Duration.ofMinutes(5);
    public static final Duration NULL_CACHE_TTL = Duration.ofMinutes(2);
    public static final Duration LOCK_TTL = Duration.ofSeconds(10);
    public static final Duration REDIS_PHYSICAL_TTL = Duration.ofDays(1);

    private static final String MATERIAL_DETAIL_PREFIX = "cache:material:detail:";
    private static final String MATERIAL_LIST_PREFIX = "cache:material:list:";
    private static final String INVENTORY_LIST_PREFIX = "cache:inventory:list:";

    private final RedisJsonCacheService cacheService;

    public CacheKeyManager(RedisJsonCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public String materialDetailKey(String materialId) {
        return MATERIAL_DETAIL_PREFIX + normalize(materialId);
    }

    public String materialListKey(String keyword, Integer status) {
        return MATERIAL_LIST_PREFIX + "keyword=" + normalize(keyword) + "|status=" + (status == null ? "null" : status);
    }

    public String inventoryListKey(String keyword) {
        return INVENTORY_LIST_PREFIX + "keyword=" + normalize(keyword);
    }

    public String lockKey(String cacheKey) {
        return cacheKey + ":lock";
    }

    public void evictMaterialCaches(String materialId) {
        cacheService.delete(materialDetailKey(materialId));
        cacheService.deleteByPrefix(MATERIAL_LIST_PREFIX);
    }

    public void evictInventoryCaches() {
        cacheService.deleteByPrefix(INVENTORY_LIST_PREFIX);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
