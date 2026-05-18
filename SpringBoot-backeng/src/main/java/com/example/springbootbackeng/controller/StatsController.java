package com.example.springbootbackeng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.service.UserService;

/**
 * 统计数据接口控制器
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private UserService userService;

    /**
     * 获取总用户数
     */
    @GetMapping("/users-count")
    public Result<?> getUsersCount() {
        long totalUsers = userService.getTotalUserCount();
        return Result.success(totalUsers);
    }
}
