package com.example.springbootbackeng.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // 匹配上下文路径 /api
public class HelloController {

    // 处理 GET /api/hello 请求
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot Backend!";
    }
}