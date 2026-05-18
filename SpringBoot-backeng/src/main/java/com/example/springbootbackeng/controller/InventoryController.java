package com.example.springbootbackeng.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.entity.Inventory;
import com.example.springbootbackeng.entity.InventoryVO;
import com.example.springbootbackeng.security.SecurityUserContext;
import com.example.springbootbackeng.service.InventoryService;
import com.example.springbootbackeng.service.OperationAuditLogger;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/inventories")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserService userService;
    private final OperationAuditLogger operationAuditLogger;

    public InventoryController(InventoryService inventoryService,
                               UserService userService,
                               OperationAuditLogger operationAuditLogger) {
        this.inventoryService = inventoryService;
        this.userService = userService;
        this.operationAuditLogger = operationAuditLogger;
    }

    private boolean hasManagePermission(HttpServletRequest request) {
        String roleCode = request.getHeader("X-Role-Code");
        String userId = request.getHeader("X-User-Id");

        boolean manager = userService.isManagerRoleCode(roleCode) && userService.isManagerByUserId(userId);
        boolean warehouse = userService.isWarehouseRoleCode(roleCode) && userService.isWarehouseByUserId(userId);
        return manager || warehouse;
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        List<InventoryVO> all = inventoryService.listAll(keyword);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<InventoryVO> records = from >= total
                ? Collections.emptyList()
                : all.subList(from, to);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", records);
        pageData.put("total", total);
        pageData.put("pageNum", validPageNum);
        pageData.put("pageSize", validPageSize);

        return Result.success(pageData);
    }

    @PostMapping
    public Result<?> create(@RequestBody Inventory inventory, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String operatorId = SecurityUserContext.getCurrentUserId();
        if (operatorId == null || operatorId.trim().isEmpty()) {
            operatorId = request.getHeader("X-User-Id");
        }
        String msg = inventoryService.create(inventory, operatorId);
        if (msg == null) {
            operationAuditLogger.log(
                    operatorId,
                    "库存管理",
                    "ADD",
                    "新增库存记录: " + inventory.getInventoryId(),
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("新增库存成功") : Result.error(msg);
    }

    @PutMapping("/{inventoryId}")
    public Result<?> update(@PathVariable String inventoryId,
                            @RequestBody Inventory inventory,
                            HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String operatorId = SecurityUserContext.getCurrentUserId();
        if (operatorId == null || operatorId.trim().isEmpty()) {
            operatorId = request.getHeader("X-User-Id");
        }
        String msg = inventoryService.update(inventoryId, inventory, operatorId);
        if (msg == null) {
            operationAuditLogger.log(
                    operatorId,
                    "库存管理",
                    "EDIT",
                    "修改库存记录: " + inventoryId,
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("更新库存成功") : Result.error(msg);
    }

    @DeleteMapping("/{inventoryId}")
    public Result<?> delete(@PathVariable String inventoryId, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String operatorId = SecurityUserContext.getCurrentUserId();
        if (operatorId == null || operatorId.trim().isEmpty()) {
            operatorId = request.getHeader("X-User-Id");
        }
        String msg = inventoryService.delete(inventoryId, operatorId);
        return msg == null ? Result.success("删除库存成功") : Result.error(msg);
    }
}
