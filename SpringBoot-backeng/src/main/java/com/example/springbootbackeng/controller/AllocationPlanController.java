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
import com.example.springbootbackeng.entity.AllocationPlan;
import com.example.springbootbackeng.entity.AllocationPlanVO;
import com.example.springbootbackeng.service.AllocationPlanService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/allocation-plans")
public class AllocationPlanController {

    private final AllocationPlanService allocationPlanService;

    public AllocationPlanController(AllocationPlanService allocationPlanService) {
        this.allocationPlanService = allocationPlanService;
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) Integer confirmStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        List<AllocationPlanVO> all = allocationPlanService.listAll(confirmStatus, keyword);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<AllocationPlanVO> records = from >= total
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
    public Result<?> create(@RequestBody AllocationPlan plan) {
        try {
            String msg = allocationPlanService.create(plan);
            return msg == null ? Result.success("新增分配方案成功") : Result.error(msg);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            return Result.error((msg == null || msg.trim().isEmpty()) ? "新增分配方案失败，事务已回滚" : msg);
        }
    }

    @GetMapping("/options/demands")
    public Result<?> demandOptions() {
        return Result.success(allocationPlanService.listDemandOptions());
    }

    @GetMapping("/options/allocators")
    public Result<?> allocatorOptions() {
        return Result.success(allocationPlanService.listAllocatorOptions());
    }
}
