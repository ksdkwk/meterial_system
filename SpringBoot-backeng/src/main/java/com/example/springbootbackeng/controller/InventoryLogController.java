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
import com.example.springbootbackeng.entity.InventoryLog;
import com.example.springbootbackeng.entity.InventoryLogVO;
import com.example.springbootbackeng.service.InventoryLogService;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/inventory-logs")
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;
    private final UserService userService;

    public InventoryLogController(InventoryLogService inventoryLogService, UserService userService) {
        this.inventoryLogService = inventoryLogService;
        this.userService = userService;
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
            @RequestParam(required = false) Integer changeType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        List<InventoryLogVO> all = inventoryLogService.listAll(changeType, action, keyword);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<InventoryLogVO> records = from >= total
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
    public Result<?> create(@RequestBody InventoryLog log, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = inventoryLogService.create(log);
        return msg == null ? Result.success("新增库存日志成功") : Result.error(msg);
    }

    @PutMapping("/{logId}")
    public Result<?> update(@PathVariable String logId,
                            @RequestBody InventoryLog log,
                            HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = inventoryLogService.update(logId, log);
        return msg == null ? Result.success("更新库存日志成功") : Result.error(msg);
    }

    @DeleteMapping("/{logId}")
    public Result<?> delete(@PathVariable String logId, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String userId = request.getHeader("X-User-Id");
        String msg = inventoryLogService.softDelete(logId, userId);
        return msg == null ? Result.success("库存日志已设为失效") : Result.error(msg);
    }
}
