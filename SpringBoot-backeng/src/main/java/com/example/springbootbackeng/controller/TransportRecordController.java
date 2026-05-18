package com.example.springbootbackeng.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.entity.TransportRecord;
import com.example.springbootbackeng.entity.TransportRecordVO;
import com.example.springbootbackeng.service.TransportRecordService;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/transport-records")
public class TransportRecordController {

    private final TransportRecordService transportRecordService;
    private final UserService userService;

    public TransportRecordController(TransportRecordService transportRecordService,
                                     UserService userService) {
        this.transportRecordService = transportRecordService;
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
            @RequestParam(required = false) Integer transportStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        List<TransportRecordVO> all = transportRecordService.listAll(transportStatus, keyword);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<TransportRecordVO> records = from >= total
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
    public Result<?> create(@RequestBody TransportRecord record, HttpServletRequest request) {
        if (!hasManagePermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = transportRecordService.create(record);
        return msg == null ? Result.success("新增运输记录成功") : Result.error(msg);
    }

    @GetMapping("/options/plans")
    public Result<?> planOptions() {
        return Result.success(transportRecordService.listAvailablePlanOptions());
    }

    @GetMapping("/options/vehicles")
    public Result<?> vehicleOptions() {
        return Result.success(transportRecordService.listVehicleOptions());
    }
}
