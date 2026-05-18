package com.example.springbootbackeng.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.cache.CacheKeyManager;
import com.example.springbootbackeng.cache.CacheMetricsService;
import com.example.springbootbackeng.cache.RedisJsonCacheService;
import com.example.springbootbackeng.entity.Inventory;
import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.InventoryLogAction;
import com.example.springbootbackeng.entity.InventoryVO;
import com.example.springbootbackeng.mapper.InventoryLogMapper;
import com.example.springbootbackeng.mapper.InventoryMapper;
import com.example.springbootbackeng.mq.MqEventPublisher;
import com.example.springbootbackeng.mq.OperationLogMessage;
import com.example.springbootbackeng.service.InventoryService;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String METRIC_INVENTORY_LIST = "inventory:list";

    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final RedisJsonCacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    private final CacheMetricsService cacheMetricsService;
    private final MqEventPublisher mqEventPublisher;

    public InventoryServiceImpl(InventoryMapper inventoryMapper,
                                InventoryLogMapper inventoryLogMapper,
                                RedisJsonCacheService cacheService,
                                CacheKeyManager cacheKeyManager,
                                CacheMetricsService cacheMetricsService,
                                MqEventPublisher mqEventPublisher) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
        this.cacheMetricsService = cacheMetricsService;
        this.mqEventPublisher = mqEventPublisher;
    }

    @Override
    public List<InventoryVO> listAll(String keyword) {
        String cacheKey = cacheKeyManager.inventoryListKey(keyword);
        RedisJsonCacheService.LogicalCacheValue<List<InventoryVO>> logicalCached =
                cacheService.getLogical(cacheKey, new TypeReference<List<InventoryVO>>() {});
        if (logicalCached != null && logicalCached.getData() != null) {
            cacheMetricsService.recordHit(METRIC_INVENTORY_LIST);
            if (logicalCached.isExpired()) {
                tryRebuildInventoryListAsync(keyword, cacheKey);
            }
            return logicalCached.getData();
        }
        if (cacheService.isNullMarker(cacheKey)) {
            cacheMetricsService.recordNullHit(METRIC_INVENTORY_LIST);
            return List.of();
        }
        cacheMetricsService.recordMiss(METRIC_INVENTORY_LIST);

        String lockKey = cacheKeyManager.lockKey(cacheKey);
        String token = UUID.randomUUID().toString();
        boolean locked = cacheService.tryLock(lockKey, token, CacheKeyManager.LOCK_TTL);
        if (locked) {
            try {
                logicalCached = cacheService.getLogical(cacheKey, new TypeReference<List<InventoryVO>>() {});
                if (logicalCached != null && logicalCached.getData() != null) {
                    cacheMetricsService.recordHit(METRIC_INVENTORY_LIST);
                    return logicalCached.getData();
                }
                cacheMetricsService.recordDbFallback(METRIC_INVENTORY_LIST);
                List<InventoryVO> dbResult = inventoryMapper.selectAllWithDetails(keyword);
                if (dbResult == null || dbResult.isEmpty()) {
                    cacheService.setNull(cacheKey, CacheKeyManager.NULL_CACHE_TTL);
                    return List.of();
                }
                cacheService.setWithLogicalExpire(
                        cacheKey,
                        dbResult,
                        CacheKeyManager.INVENTORY_LIST_TTL,
                        CacheKeyManager.REDIS_PHYSICAL_TTL
                );
                return dbResult;
            } finally {
                cacheService.unlock(lockKey, token);
            }
        }

        for (int i = 0; i < 5; i++) {
            sleepSilently(40L);
            logicalCached = cacheService.getLogical(cacheKey, new TypeReference<List<InventoryVO>>() {});
            if (logicalCached != null && logicalCached.getData() != null) {
                cacheMetricsService.recordHit(METRIC_INVENTORY_LIST);
                return logicalCached.getData();
            }
            if (cacheService.isNullMarker(cacheKey)) {
                cacheMetricsService.recordNullHit(METRIC_INVENTORY_LIST);
                return List.of();
            }
        }

        cacheMetricsService.recordDbFallback(METRIC_INVENTORY_LIST);
        List<InventoryVO> dbResult = inventoryMapper.selectAllWithDetails(keyword);
        if (dbResult == null || dbResult.isEmpty()) {
            cacheService.setNull(cacheKey, CacheKeyManager.NULL_CACHE_TTL);
            return List.of();
        }
        cacheService.setWithLogicalExpire(
                cacheKey,
                dbResult,
                CacheKeyManager.INVENTORY_LIST_TTL,
                CacheKeyManager.REDIS_PHYSICAL_TTL
        );
        return dbResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(Inventory inventory, String operatorId) {
        if (inventory == null) {
            return "请求参数不能为空";
        }
        if (inventory.getMaterialId() == null || inventory.getMaterialId().trim().isEmpty()) {
            return "物料ID不能为空";
        }
        if (inventory.getStorageLocation() == null || inventory.getStorageLocation().trim().isEmpty()) {
            return "存储位置不能为空";
        }
        if (inventory.getStockQuantity() == null || inventory.getStockQuantity() < 0) {
            return "库存数量不能小于0";
        }
        if (operatorId == null || operatorId.trim().isEmpty()) {
            return "操作人不能为空";
        }

        Long exists = inventoryMapper.selectCount(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, inventory.getMaterialId().trim())
                .eq(Inventory::getStorageLocation, inventory.getStorageLocation().trim()));
        if (exists != null && exists > 0) {
            return "同一物料在该位置的库存记录已存在";
        }

        if (inventory.getInventoryId() == null || inventory.getInventoryId().trim().isEmpty()) {
            inventory.setInventoryId(UUID.randomUUID().toString().replace("-", ""));
        }
        inventory.setMaterialId(inventory.getMaterialId().trim());
        inventory.setStorageLocation(inventory.getStorageLocation().trim());
        int insert = inventoryMapper.insert(inventory);
        if (insert <= 0) {
            return "新增库存失败";
        }

        int afterQuantity = safeInt(inventory.getStockQuantity());
        saveInventoryLog(
                inventory.getInventoryId(),
                inventory.getMaterialId(),
                1,
                afterQuantity,
                0,
                afterQuantity,
                "新增库存",
                InventoryLogAction.ADD.getCode(),
                operatorId
        );

        cacheKeyManager.evictInventoryCaches();
        publishOperationLog(operatorId, "ADD", "新增库存记录: " + inventory.getInventoryId());

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String update(String inventoryId, Inventory inventory, String operatorId) {
        if (inventoryId == null || inventoryId.trim().isEmpty()) {
            return "库存ID不能为空";
        }
        Inventory old = inventoryMapper.selectById(inventoryId);
        if (old == null) {
            return "库存记录不存在";
        }
        if (inventory == null) {
            return "请求参数不能为空";
        }

        String materialId = inventory.getMaterialId();
        if (materialId == null || materialId.trim().isEmpty()) {
            return "物料ID不能为空";
        }
        String storageLocation = inventory.getStorageLocation();
        if (storageLocation == null || storageLocation.trim().isEmpty()) {
            return "存储位置不能为空";
        }
        if (inventory.getStockQuantity() == null || inventory.getStockQuantity() < 0) {
            return "库存数量不能小于0";
        }
        if (operatorId == null || operatorId.trim().isEmpty()) {
            return "操作人不能为空";
        }

        Long exists = inventoryMapper.selectCount(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId.trim())
                .eq(Inventory::getStorageLocation, storageLocation.trim())
                .ne(Inventory::getInventoryId, inventoryId));
        if (exists != null && exists > 0) {
            return "同一物料在该位置的库存记录已存在";
        }

        inventory.setInventoryId(inventoryId);
        inventory.setMaterialId(materialId.trim());
        inventory.setStorageLocation(storageLocation.trim());
        int beforeQuantity = safeInt(old.getStockQuantity());
        int afterQuantity = safeInt(inventory.getStockQuantity());
        int changed = inventoryMapper.updateById(inventory);
        if (changed <= 0) {
            return "更新库存失败";
        }

        int diff = afterQuantity - beforeQuantity;
        int changeType = diff > 0 ? 1 : (diff < 0 ? 2 : 3);
        String reason = diff == 0
            ? "库存信息编辑(数量未变化)"
            : "库存数量调整";
        saveInventoryLog(
            inventoryId,
            inventory.getMaterialId(),
            changeType,
            Math.abs(diff),
            beforeQuantity,
            afterQuantity,
            reason,
            InventoryLogAction.ADJUST.getCode(),
            operatorId
        );

        cacheKeyManager.evictInventoryCaches();
        publishOperationLog(operatorId, "EDIT", "更新库存记录: " + inventoryId);

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delete(String inventoryId, String operatorId) {
        if (inventoryId == null || inventoryId.trim().isEmpty()) {
            return "库存ID不能为空";
        }
        Inventory old = inventoryMapper.selectById(inventoryId);
        if (old == null) {
            return "库存记录不存在";
        }
        if (operatorId == null || operatorId.trim().isEmpty()) {
            return "操作人不能为空";
        }

        int beforeQuantity = safeInt(old.getStockQuantity());
        int deleted = inventoryMapper.deleteById(inventoryId);
        if (deleted <= 0) {
            return "删除库存失败";
        }

        saveInventoryLog(
                inventoryId,
                old.getMaterialId(),
                2,
                beforeQuantity,
                beforeQuantity,
                0,
                "删除库存记录(撤回新增或清理)",
                InventoryLogAction.REVOKE_ADD.getCode(),
                operatorId
        );

        cacheKeyManager.evictInventoryCaches();
        publishOperationLog(operatorId, "DELETE", "删除库存记录: " + inventoryId);

        return null;
    }

    private void saveInventoryLog(String inventoryId,
                                  String materialId,
                                  Integer changeType,
                                  Integer changeQuantity,
                                  Integer beforeQuantity,
                                  Integer afterQuantity,
                                  String reason,
                                  String logChange,
                                  String operatorId) {
        InventoryLog log = new InventoryLog();
        log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        log.setInventoryId(inventoryId);
        log.setMaterialId(materialId);
        log.setChangeType(changeType);
        log.setChangeQuantity(changeQuantity == null ? 0 : changeQuantity);
        log.setBeforeQuantity(beforeQuantity == null ? 0 : beforeQuantity);
        log.setAfterQuantity(afterQuantity == null ? 0 : afterQuantity);
        log.setReason(reason);
        log.setLogChange(logChange);
        log.setOperatorId(operatorId.trim());
        log.setOperateTime(LocalDateTime.now());
        log.setLogStatus(1);
        if (inventoryLogMapper.insert(log) <= 0) {
            throw new IllegalStateException("库存日志写入失败");
        }
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private void tryRebuildInventoryListAsync(String keyword, String cacheKey) {
        String lockKey = cacheKeyManager.lockKey(cacheKey);
        String token = UUID.randomUUID().toString();
        boolean locked = cacheService.tryLock(lockKey, token, CacheKeyManager.LOCK_TTL);
        if (!locked) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            long begin = System.nanoTime();
            try {
                List<InventoryVO> latest = inventoryMapper.selectAllWithDetails(keyword);
                if (latest == null || latest.isEmpty()) {
                    cacheService.setNull(cacheKey, CacheKeyManager.NULL_CACHE_TTL);
                    return;
                }
                cacheService.setWithLogicalExpire(
                        cacheKey,
                        latest,
                        CacheKeyManager.INVENTORY_LIST_TTL,
                        CacheKeyManager.REDIS_PHYSICAL_TTL
                );
            } finally {
                cacheMetricsService.recordRebuild(METRIC_INVENTORY_LIST, System.nanoTime() - begin);
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

    private void publishOperationLog(String userId, String opType, String desc) {
        OperationLogMessage message = new OperationLogMessage();
        message.setUserId(userId);
        message.setOperationModule("库存管理");
        message.setOperationType(opType);
        message.setOperationDesc(desc);
        message.setRequestIp(null);
        mqEventPublisher.publishOperationLog(message);
    }
}
