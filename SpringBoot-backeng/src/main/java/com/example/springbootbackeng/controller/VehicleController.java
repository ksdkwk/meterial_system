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
import com.example.springbootbackeng.entity.Vehicle;
import com.example.springbootbackeng.service.OperationAuditLogger;
import com.example.springbootbackeng.service.UserService;
import com.example.springbootbackeng.service.VehicleService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserService userService;
    private final OperationAuditLogger operationAuditLogger;

    public VehicleController(VehicleService vehicleService,
                             UserService userService,
                             OperationAuditLogger operationAuditLogger) {
        this.vehicleService = vehicleService;
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
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        List<Vehicle> all = vehicleService.list(keyword, status);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<Vehicle> records = from >= total
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
    public Result<?> create(@RequestBody Vehicle vehicle, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = vehicleService.create(vehicle);
        if (msg == null) {
            operationAuditLogger.log(
                    request.getHeader("X-User-Id"),
                    "车辆管理",
                    "ADD",
                    "新增车辆: " + vehicle.getVehicleId(),
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("新增车辆成功") : Result.error(msg);
    }

    @PutMapping("/{vehicleId}")
    public Result<?> update(@PathVariable String vehicleId,
                            @RequestBody Vehicle vehicle,
                            HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = vehicleService.update(vehicleId, vehicle);
        if (msg == null) {
            operationAuditLogger.log(
                    request.getHeader("X-User-Id"),
                    "车辆管理",
                    "EDIT",
                    "修改车辆: " + vehicleId,
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("更新车辆成功") : Result.error(msg);
    }

    @DeleteMapping("/{vehicleId}")
    public Result<?> delete(@PathVariable String vehicleId, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = vehicleService.delete(vehicleId);
        return msg == null ? Result.success("删除车辆成功") : Result.error(msg);
    }
}
