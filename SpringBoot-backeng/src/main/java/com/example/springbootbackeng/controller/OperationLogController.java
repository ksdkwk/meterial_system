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
import com.example.springbootbackeng.entity.OperationLog;
import com.example.springbootbackeng.entity.OperationLogVO;
import com.example.springbootbackeng.service.OperationLogService;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final UserService userService;

    public OperationLogController(OperationLogService operationLogService, UserService userService) {
        this.operationLogService = operationLogService;
        this.userService = userService;
    }

    private boolean isManager(HttpServletRequest request) {
        String roleCode = request.getHeader("X-Role-Code");
        String userId = request.getHeader("X-User-Id");
        return userService.isManagerRoleCode(roleCode) && userService.isManagerByUserId(userId);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String operationType,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          HttpServletRequest request) {
        if (!isManager(request)) {
            return Result.error("无权限查看系统操作日志");
        }

        List<OperationLogVO> all = operationLogService.listAll(keyword, operationType);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<OperationLogVO> records = from >= total ? Collections.emptyList() : all.subList(from, to);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", records);
        pageData.put("total", total);
        pageData.put("pageNum", validPageNum);
        pageData.put("pageSize", validPageSize);

        return Result.success(pageData);
    }

    @PostMapping
    public Result<?> create(@RequestBody OperationLog log, HttpServletRequest request) {
        if (!isManager(request)) {
            return Result.error("无权限新增系统操作日志");
        }
        String msg = operationLogService.create(log);
        return msg == null ? Result.success("新增系统操作日志成功") : Result.error(msg);
    }
}
