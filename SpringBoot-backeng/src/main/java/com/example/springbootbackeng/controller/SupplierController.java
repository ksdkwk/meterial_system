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
import com.example.springbootbackeng.entity.Supplier;
import com.example.springbootbackeng.service.OperationAuditLogger;
import com.example.springbootbackeng.service.SupplierService;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final UserService userService;
    private final OperationAuditLogger operationAuditLogger;

    public SupplierController(SupplierService supplierService,
                              UserService userService,
                              OperationAuditLogger operationAuditLogger) {
        this.supplierService = supplierService;
        this.userService = userService;
        this.operationAuditLogger = operationAuditLogger;
    }

    private boolean hasManagerPermission(HttpServletRequest request) {
        String roleCode = request.getHeader("X-Role-Code");
        String userId = request.getHeader("X-User-Id");

        boolean manager = userService.isManagerRoleCode(roleCode) && userService.isManagerByUserId(userId);
        boolean warehouse = userService.isWarehouseRoleCode(roleCode) && userService.isWarehouseByUserId(userId);
        return manager || warehouse;
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        List<Supplier> all = supplierService.list(keyword, status);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<Supplier> records = from >= total
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
    public Result<?> create(@RequestBody Supplier supplier, HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = supplierService.create(supplier);
        if (msg == null) {
            operationAuditLogger.log(
                    request.getHeader("X-User-Id"),
                    "供应商管理",
                    "ADD",
                    "新增供应商: " + supplier.getSupplierId(),
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("新增供应商成功") : Result.error(msg);
    }

    @PutMapping("/{supplierId}")
    public Result<?> update(@PathVariable String supplierId,
                            @RequestBody Supplier supplier,
                            HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = supplierService.update(supplierId, supplier);
        if (msg == null) {
            operationAuditLogger.log(
                    request.getHeader("X-User-Id"),
                    "供应商管理",
                    "EDIT",
                    "修改供应商: " + supplierId,
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("更新供应商成功") : Result.error(msg);
    }

    @DeleteMapping("/{supplierId}")
    public Result<?> delete(@PathVariable String supplierId, HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = supplierService.delete(supplierId);
        return msg == null ? Result.success("删除供应商成功") : Result.error(msg);
    }
}
