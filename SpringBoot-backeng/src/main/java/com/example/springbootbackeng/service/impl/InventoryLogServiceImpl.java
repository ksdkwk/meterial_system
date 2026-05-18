package com.example.springbootbackeng.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.InventoryLogAction;
import com.example.springbootbackeng.entity.InventoryLogVO;
import com.example.springbootbackeng.mapper.InventoryLogMapper;
import com.example.springbootbackeng.service.InventoryLogService;

@Service
public class InventoryLogServiceImpl implements InventoryLogService {

    private final InventoryLogMapper inventoryLogMapper;

    public InventoryLogServiceImpl(InventoryLogMapper inventoryLogMapper) {
        this.inventoryLogMapper = inventoryLogMapper;
    }

    @Override
    public List<InventoryLogVO> listAll(Integer changeType, String action, String keyword) {
        String actionCode = normalizeActionCode(action);
        return inventoryLogMapper.selectAllWithDetails(changeType, actionCode, keyword);
    }

    @Override
    public String create(InventoryLog log) {
        if (log == null) return "请求参数不能为空";
        if (isBlank(log.getInventoryId())) return "库存ID不能为空";
        if (isBlank(log.getMaterialId())) return "物料ID不能为空";
        if (log.getChangeType() == null) return "变动类型不能为空";
        if (log.getChangeQuantity() == null) return "变动数量不能为空";
        if (log.getBeforeQuantity() == null) return "变动前数量不能为空";
        if (log.getAfterQuantity() == null) return "变动后数量不能为空";
        if (isBlank(log.getOperatorId())) return "操作人ID不能为空";

        String normalizedAction = normalizeActionCode(log.getLogChange());
        if (normalizedAction == null) {
            normalizedAction = defaultActionCodeByType(log.getChangeType());
        }

        if (isBlank(log.getLogId())) {
            log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        }
        log.setLogChange(normalizedAction);
        if (log.getLogStatus() == null) {
            log.setLogStatus(1);
        }

        return inventoryLogMapper.insert(log) > 0 ? null : "新增库存日志失败";
    }

    @Override
    public String update(String logId, InventoryLog log) {
        if (isBlank(logId)) return "日志ID不能为空";
        InventoryLog old = inventoryLogMapper.selectById(logId);
        if (old == null) return "日志不存在";
        if (old.getLogStatus() != null && old.getLogStatus() == 0) return "日志已失效，不可编辑";
        if (log == null) return "请求参数不能为空";

        if (isBlank(log.getInventoryId())) return "库存ID不能为空";
        if (isBlank(log.getMaterialId())) return "物料ID不能为空";
        if (log.getChangeType() == null) return "变动类型不能为空";
        if (log.getChangeQuantity() == null) return "变动数量不能为空";
        if (log.getBeforeQuantity() == null) return "变动前数量不能为空";
        if (log.getAfterQuantity() == null) return "变动后数量不能为空";
        if (isBlank(log.getOperatorId())) return "操作人ID不能为空";

        String normalizedAction = normalizeActionCode(log.getLogChange());
        if (normalizedAction == null) {
            return "日志业务动作不合法，应为 ADD/DELETE/REVOKE_ADD/ALLOCATE/ADJUST";
        }

        log.setLogId(logId);
        log.setLogChange(normalizedAction);
        if (log.getLogStatus() == null) {
            Integer oldStatus = old.getLogStatus();
            log.setLogStatus(oldStatus == null ? Integer.valueOf(1) : oldStatus);
        }

        return inventoryLogMapper.updateById(log) > 0 ? null : "更新库存日志失败";
    }

    @Override
    public String softDelete(String logId, String operatorId) {
        if (isBlank(logId)) return "日志ID不能为空";
        InventoryLog old = inventoryLogMapper.selectById(logId);
        if (old == null) return "日志不存在";
        if (old.getLogStatus() != null && old.getLogStatus() == 0) return null;

        InventoryLog patch = new InventoryLog();
        patch.setLogId(logId);
        patch.setLogStatus(0);

        return inventoryLogMapper.updateById(patch) > 0 ? null : "日志失效处理失败";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String normalizeActionCode(String raw) {
        InventoryLogAction action = InventoryLogAction.fromAny(raw);
        return action == null ? null : action.getCode();
    }

    private String defaultActionCodeByType(Integer changeType) {
        int type = changeType == null ? 0 : changeType;
        if (type == 1 || type == 4) {
            return InventoryLogAction.ADD.getCode();
        }
        if (type == 2) {
            return InventoryLogAction.DELETE.getCode();
        }
        return InventoryLogAction.ADJUST.getCode();
    }
}
