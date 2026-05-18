package com.example.springbootbackeng.cache;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.entity.Material;
import com.example.springbootbackeng.mapper.MaterialMapper;
import com.example.springbootbackeng.service.InventoryService;
import com.example.springbootbackeng.service.MaterialService;

@Component
public class CacheWarmupRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final MaterialService materialService;
    private final InventoryService inventoryService;
    private final MaterialMapper materialMapper;
    private final CacheBloomService cacheBloomService;

    @Value("${app.cache.warmup.enabled:true}")
    private boolean warmupEnabled;

    @Value("${app.cache.warmup.material-detail-limit:20}")
    private int materialDetailLimit;

    public CacheWarmupRunner(MaterialService materialService,
                             InventoryService inventoryService,
                             MaterialMapper materialMapper,
                             CacheBloomService cacheBloomService) {
        this.materialService = materialService;
        this.inventoryService = inventoryService;
        this.materialMapper = materialMapper;
        this.cacheBloomService = cacheBloomService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        if (!warmupEnabled) {
            return;
        }
        long begin = System.nanoTime();
        try {
            // 列表热点预热
            materialService.list(null, 1);
            inventoryService.listAll(null);

            // 详情热点预热（最近活跃物料）
            int limit = Math.max(1, materialDetailLimit);
            List<Material> topMaterials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                    .eq(Material::getStatus, 1)
                    .orderByDesc(Material::getUpdateTime)
                    .last("LIMIT " + limit));

            // 布隆过滤器预热（穿透保护）
            if (cacheBloomService.enabled()) {
                List<Material> allMaterials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .select(Material::getMaterialId));
                for (Material material : allMaterials) {
                    cacheBloomService.addMaterialId(material.getMaterialId());
                }
            }

            for (Material material : topMaterials) {
                if (material.getMaterialId() != null && !material.getMaterialId().trim().isEmpty()) {
                    materialService.getById(material.getMaterialId());
                }
            }
            double costMs = (System.nanoTime() - begin) / 1_000_000.0D;
            log.info("缓存预热完成：materialList + inventoryList + materialDetail({})，耗时={}ms", topMaterials.size(), String.format("%.2f", costMs));
        } catch (Exception ex) {
            log.warn("缓存预热执行异常，已忽略，不影响系统启动", ex);
        }
    }
}
