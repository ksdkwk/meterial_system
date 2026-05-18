package com.example.springbootbackeng.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.dto.AiAnalysisRunRequest;
import com.example.springbootbackeng.service.AiAnalysisResultService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/ai-analysis/inventory")
public class AiAnalysisResultController {

    private final AiAnalysisResultService aiAnalysisResultService;

    public AiAnalysisResultController(AiAnalysisResultService aiAnalysisResultService) {
        this.aiAnalysisResultService = aiAnalysisResultService;
    }

    @GetMapping("/config-check")
    public Result<?> configCheck() {
        return Result.success(aiAnalysisResultService.getAiConfigCheck());
    }

    @PostMapping("/run")
    public Result<?> run(@RequestBody AiAnalysisRunRequest request) {
        try {
            return Result.success(aiAnalysisResultService.runInventoryAnalysis(request));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("AI分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer analysisType,
                          @RequestParam(required = false) Integer timeGranularity,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(aiAnalysisResultService.listResults(analysisType, timeGranularity, pageNum, pageSize));
    }

    @GetMapping("/result")
    public Result<?> getRawResult(@RequestParam String analysisId) {
        if (analysisId == null || analysisId.trim().isEmpty()) {
            return Result.error("analysisId不能为空");
        }
        var payload = aiAnalysisResultService.getRawResult(analysisId.trim());
        if (payload == null) {
            return Result.error("未找到对应的AI分析结果");
        }
        return Result.success(payload);
    }
}
