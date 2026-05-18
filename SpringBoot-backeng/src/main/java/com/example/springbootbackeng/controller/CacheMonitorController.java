package com.example.springbootbackeng.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.cache.CacheMetricsService;
import com.example.springbootbackeng.common.Result;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/cache")
public class CacheMonitorController {

    private final CacheMetricsService cacheMetricsService;

    public CacheMonitorController(CacheMetricsService cacheMetricsService) {
        this.cacheMetricsService = cacheMetricsService;
    }

    @GetMapping("/metrics")
    public Result<?> metrics() {
        return Result.success(cacheMetricsService.snapshot());
    }
}
