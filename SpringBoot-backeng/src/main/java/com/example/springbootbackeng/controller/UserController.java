package com.example.springbootbackeng.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户管理接口
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    private boolean hasManagerPermission(HttpServletRequest request) {
        Object loginUserObj = request.getSession().getAttribute("loginUser");
        if (loginUserObj instanceof User loginUser) {
            return userService.isManagerByUserId(loginUser.getUserId());
        }

        String roleCode = request.getHeader("X-Role-Code");
        String userId = request.getHeader("X-User-Id");

        if (!userService.isManagerRoleCode(roleCode)) {
            return false;
        }
        return userService.isManagerByUserId(userId);
    }

    private Result<?> buildUserListResult(String keyword, Integer roleId, Integer status,
                                          Integer pageNum, Integer pageSize,
                                          String sortBy, String sortOrder) {
        List<User> users = userService.getUserList(keyword, roleId, status, sortBy, sortOrder);

        int total = users.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);
        List<User> records = from >= total ? Collections.emptyList() : users.subList(from, to);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", records);
        pageData.put("total", total);
        pageData.put("pageNum", validPageNum);
        pageData.put("pageSize", validPageSize);
        return Result.success(pageData);
    }

    @GetMapping("/list")
    public Result<?> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            HttpServletRequest request) {

        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        return buildUserListResult(keyword, roleId, status, pageNum, pageSize, sortBy, sortOrder);
    }

    @GetMapping
    public Result<?> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            HttpServletRequest request) {

        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        return buildUserListResult(keyword, roleId, status, pageNum, pageSize, sortBy, sortOrder);
    }

    @PostMapping
    public Result<?> createUser(@RequestBody User user, HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = userService.createUser(user);
        return msg == null ? Result.success(true) : Result.error(msg);
    }

    @PutMapping("/{userId}")
    public Result<?> updateUser(@PathVariable String userId, @RequestBody User user, HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        String msg = userService.updateUser(userId, user);
        return msg == null ? Result.success(true) : Result.error(msg);
    }

    @DeleteMapping("/{userId}")
    public Result<?> deleteUser(@PathVariable String userId, HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        boolean ok = userService.deleteUser(userId);
        return ok ? Result.success(true) : Result.error("删除用户失败");
    }

    @DeleteMapping("/batch")
    public Result<?> batchDeleteUsers(@RequestBody List<String> userIds, HttpServletRequest request) {
        if (!hasManagerPermission(request)) {
            return Result.error("无权限执行该操作");
        }
        boolean ok = userService.batchDeleteUsers(userIds);
        return ok ? Result.success(true) : Result.error("批量删除失败");
    }
}
