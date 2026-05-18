package com.example.springbootbackeng.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.cache.CacheKeyManager;
import com.example.springbootbackeng.entity.AllocationPlan;
import com.example.springbootbackeng.entity.AllocationPlanVO;
import com.example.springbootbackeng.entity.Inventory;
import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.InventoryLogAction;
import com.example.springbootbackeng.entity.MaterialDemand;
import com.example.springbootbackeng.entity.SimpleOption;
import com.example.springbootbackeng.mapper.AllocationPlanMapper;
import com.example.springbootbackeng.mapper.InventoryLogMapper;
import com.example.springbootbackeng.mapper.InventoryMapper;
import com.example.springbootbackeng.mapper.MaterialDemandMapper;
import com.example.springbootbackeng.service.AllocationPlanService;

@Service
public class AllocationPlanServiceImpl implements AllocationPlanService {

    private final AllocationPlanMapper allocationPlanMapper;
    private final MaterialDemandMapper materialDemandMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final CacheKeyManager cacheKeyManager;

    public AllocationPlanServiceImpl(AllocationPlanMapper allocationPlanMapper,
                                     MaterialDemandMapper materialDemandMapper,
                                     InventoryMapper inventoryMapper,
                                     InventoryLogMapper inventoryLogMapper,
                                     CacheKeyManager cacheKeyManager) {
        this.allocationPlanMapper = allocationPlanMapper;
        this.materialDemandMapper = materialDemandMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.cacheKeyManager = cacheKeyManager;
    }

    @Override
    public List<AllocationPlanVO> listAll(Integer confirmStatus, String keyword) {
        return allocationPlanMapper.selectAllWithDetails(confirmStatus, keyword);
    }

    @Override
    public List<SimpleOption> listDemandOptions() {
        return allocationPlanMapper.selectDemandOptions();
    }

    @Override
    public List<SimpleOption> listAllocatorOptions() {
        return allocationPlanMapper.selectAllocatorOptions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AllocationPlan plan) {
        if (plan == null) {
            return "请求参数不能为空";
        }
        if (plan.getDemandId() == null || plan.getDemandId().trim().isEmpty()) {
            return "需求单号不能为空";
        }
        if (plan.getAllocQuantity() == null || plan.getAllocQuantity() <= 0) {
            return "分配数量必须大于0";
        }
        if (plan.getPickLocation() == null || plan.getPickLocation().trim().isEmpty()) {
            return "领取地点不能为空";
        }
        if (plan.getPickTime() == null) {
            return "计划领取时间不能为空";
        }
        if (plan.getAllocatorId() == null || plan.getAllocatorId().trim().isEmpty()) {
            return "制定人ID不能为空";
        }

        MaterialDemand demand = materialDemandMapper.selectById(plan.getDemandId());
        if (demand == null) {
            return "关联需求单不存在";
        }
        if (demand.getDemandStatus() == null || demand.getDemandStatus() != 1) {
            return "仅审核通过的需求单可分配";
        }
        if (demand.getMaterialId() == null || demand.getMaterialId().trim().isEmpty()) {
            return "需求单未关联有效物料";
        }

        String materialId = demand.getMaterialId().trim();
        String pickLocation = plan.getPickLocation().trim();
        plan.setPickLocation(pickLocation);

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getStorageLocation, pickLocation));
        if (inventory == null) {
            return "指定领取地点无该物料库存记录，请先维护库存";
        }

        if (plan.getPlanId() == null || plan.getPlanId().trim().isEmpty()) {
            plan.setPlanId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (plan.getConfirmStatus() == null) {
            plan.setConfirmStatus(0);
        }

        int changed = inventoryMapper.decreaseStockIfEnough(inventory.getInventoryId(), plan.getAllocQuantity());
        if (changed <= 0) {
            Inventory latest = inventoryMapper.selectById(inventory.getInventoryId());
            int latestStock = latest == null ? 0 : safeInt(latest.getStockQuantity());
            return "库存不足，当前库存仅" + latestStock;
        }

        Inventory updatedInventory = inventoryMapper.selectById(inventory.getInventoryId());
        if (updatedInventory == null) {
            throw new IllegalStateException("库存记录异常，事务回滚");
        }
        int afterQuantity = safeInt(updatedInventory.getStockQuantity());
        int beforeQuantity = afterQuantity + plan.getAllocQuantity();

        if (allocationPlanMapper.insert(plan) <= 0) {
            throw new IllegalStateException("新增分配方案失败，事务回滚");
        }

        InventoryLog log = new InventoryLog();
        log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        log.setInventoryId(inventory.getInventoryId());
        log.setMaterialId(materialId);
        log.setChangeType(2); // 2-出库
        log.setChangeQuantity(plan.getAllocQuantity());
        log.setBeforeQuantity(beforeQuantity);
        log.setAfterQuantity(afterQuantity);
        log.setReason("分配方案扣减库存，方案单号: " + plan.getPlanId());
        log.setLogChange(InventoryLogAction.ALLOCATE.getCode());
        log.setOperatorId(plan.getAllocatorId());
        log.setOperateTime(LocalDateTime.now());
        log.setLogStatus(1);

        if (inventoryLogMapper.insert(log) <= 0) {
            throw new IllegalStateException("库存变动日志写入失败，事务回滚");
        }

        cacheKeyManager.evictInventoryCaches();

        return null;
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }
}
