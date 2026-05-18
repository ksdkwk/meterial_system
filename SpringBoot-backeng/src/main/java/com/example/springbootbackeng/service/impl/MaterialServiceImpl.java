package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootbackeng.cache.CacheBloomService;
import com.example.springbootbackeng.cache.CacheKeyManager;
import com.example.springbootbackeng.cache.CacheMetricsService;
import com.example.springbootbackeng.cache.RedisJsonCacheService;
import com.example.springbootbackeng.entity.Material;
import com.example.springbootbackeng.mapper.MaterialMapper;
import com.example.springbootbackeng.service.MaterialService;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    private static final String METRIC_MATERIAL_DETAIL = "material:detail";
    private static final String METRIC_MATERIAL_LIST = "material:list";

    private final RedisJsonCacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    private final CacheMetricsService cacheMetricsService;
    private final CacheBloomService cacheBloomService;

    public MaterialServiceImpl(RedisJsonCacheService cacheService,
                               CacheKeyManager cacheKeyManager,
                               CacheMetricsService cacheMetricsService,
                               CacheBloomService cacheBloomService) {
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
        this.cacheMetricsService = cacheMetricsService;
        this.cacheBloomService = cacheBloomService;
    }

    @Override
    public List<Material> list(String keyword, Integer status) {
        String cacheKey = cacheKeyManager.materialListKey(keyword, status);
        List<Material> cached = cacheService.get(cacheKey, new TypeReference<List<Material>>() {});
        if (cached != null) {
            cacheMetricsService.recordHit(METRIC_MATERIAL_LIST);
            return cached;
        }
        cacheMetricsService.recordMiss(METRIC_MATERIAL_LIST);
        cacheMetricsService.recordDbFallback(METRIC_MATERIAL_LIST);

        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.like(keyword != null && !keyword.trim().isEmpty(), Material::getMaterialName, keyword)
          .or(keyword != null && !keyword.trim().isEmpty())
          .like(keyword != null && !keyword.trim().isEmpty(), Material::getMaterialType, keyword)
          .eq(status != null, Material::getStatus, status)
          .orderByDesc(Material::getCreateTime);
        List<Material> dbResult = list(qw);
        cacheService.set(cacheKey, dbResult, CacheKeyManager.MATERIAL_LIST_TTL);
        return dbResult;
    }

    @Override
    public Material getById(String materialId) {
        if (cacheBloomService.enabled() && !cacheBloomService.mightContainMaterialId(materialId)) {
            cacheMetricsService.recordBloomReject(METRIC_MATERIAL_DETAIL);
            return null;
        }

        String cacheKey = cacheKeyManager.materialDetailKey(materialId);
        RedisJsonCacheService.LogicalCacheValue<Material> logicalCached = cacheService.getLogical(cacheKey, Material.class);
        if (logicalCached != null && logicalCached.getData() != null) {
            cacheMetricsService.recordHit(METRIC_MATERIAL_DETAIL);
            if (logicalCached.isExpired()) {
                tryRebuildMaterialCacheAsync(materialId, cacheKey);
            }
            return logicalCached.getData();
        }
        if (cacheService.isNullMarker(cacheKey)) {
            cacheMetricsService.recordNullHit(METRIC_MATERIAL_DETAIL);
            return null;
        }
        cacheMetricsService.recordMiss(METRIC_MATERIAL_DETAIL);

        String lockKey = cacheKeyManager.lockKey(cacheKey);
        String token = UUID.randomUUID().toString();
        boolean locked = cacheService.tryLock(lockKey, token, CacheKeyManager.LOCK_TTL);
        if (!locked) {
            for (int i = 0; i < 5; i++) {
                sleepSilently(50L);
                logicalCached = cacheService.getLogical(cacheKey, Material.class);
                if (logicalCached != null && logicalCached.getData() != null) {
                    cacheMetricsService.recordHit(METRIC_MATERIAL_DETAIL);
                    return logicalCached.getData();
                }
                if (cacheService.isNullMarker(cacheKey)) {
                    cacheMetricsService.recordNullHit(METRIC_MATERIAL_DETAIL);
                    return null;
                }
            }
        } else {
            try {
                logicalCached = cacheService.getLogical(cacheKey, Material.class);
                if (logicalCached != null && logicalCached.getData() != null) {
                    cacheMetricsService.recordHit(METRIC_MATERIAL_DETAIL);
                    return logicalCached.getData();
                }
                if (cacheService.isNullMarker(cacheKey)) {
                    cacheMetricsService.recordNullHit(METRIC_MATERIAL_DETAIL);
                    return null;
                }

                cacheMetricsService.recordDbFallback(METRIC_MATERIAL_DETAIL);
                Material dbResult = getBaseMapper().selectById(materialId);
                if (dbResult == null) {
                    cacheService.setNull(cacheKey, CacheKeyManager.NULL_CACHE_TTL);
                    return null;
                }
                cacheService.setWithLogicalExpire(
                        cacheKey,
                        dbResult,
                        CacheKeyManager.MATERIAL_DETAIL_TTL,
                        CacheKeyManager.REDIS_PHYSICAL_TTL
                );
                return dbResult;
            } finally {
                cacheService.unlock(lockKey, token);
            }
        }

        cacheMetricsService.recordDbFallback(METRIC_MATERIAL_DETAIL);
        Material fallback = getBaseMapper().selectById(materialId);
        if (fallback == null) {
            cacheService.setNull(cacheKey, CacheKeyManager.NULL_CACHE_TTL);
            return null;
        }
        cacheService.setWithLogicalExpire(
                cacheKey,
                fallback,
                CacheKeyManager.MATERIAL_DETAIL_TTL,
                CacheKeyManager.REDIS_PHYSICAL_TTL
        );
        return fallback;
    }

    @Override
    public boolean create(Material material) {
        if (material.getMaterialId() == null || material.getMaterialId().isEmpty()) {
            material.setMaterialId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (material.getStatus() == null) {
            material.setStatus(1);
        }
        boolean success = save(material);
        if (success) {
            cacheBloomService.addMaterialId(material.getMaterialId());
            cacheKeyManager.evictMaterialCaches(material.getMaterialId());
        }
        return success;
    }

    @Override
    public boolean update(String materialId, Material material) {
        material.setMaterialId(materialId);
        boolean success = updateById(material);
        if (success) {
            cacheKeyManager.evictMaterialCaches(materialId);
        }
        return success;
    }

    @Override
    public boolean delete(String materialId) {
        // 逻辑删除，避免被库存/需求外键引用时报错
        Material m = new Material();
        m.setMaterialId(materialId);
        m.setStatus(0);
        boolean success = updateById(m);
        if (success) {
            cacheKeyManager.evictMaterialCaches(materialId);
        }
        return success;
    }

    private void tryRebuildMaterialCacheAsync(String materialId, String cacheKey) {
        String lockKey = cacheKeyManager.lockKey(cacheKey);
        String token = UUID.randomUUID().toString();
        boolean locked = cacheService.tryLock(lockKey, token, CacheKeyManager.LOCK_TTL);
        if (!locked) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            long begin = System.nanoTime();
            try {
                Material latest = getBaseMapper().selectById(materialId);
                if (latest == null) {
                    cacheService.setNull(cacheKey, CacheKeyManager.NULL_CACHE_TTL);
                    return;
                }
                cacheService.setWithLogicalExpire(
                        cacheKey,
                        latest,
                        CacheKeyManager.MATERIAL_DETAIL_TTL,
                        CacheKeyManager.REDIS_PHYSICAL_TTL
                );
            } finally {
                cacheMetricsService.recordRebuild(METRIC_MATERIAL_DETAIL, System.nanoTime() - begin);
                cacheService.unlock(lockKey, token);
            }
        });
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}