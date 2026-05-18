package com.example.springbootbackeng.service;

import java.util.Map;

import com.example.springbootbackeng.dto.AiAnalysisRunRequest;

public interface AiAnalysisResultService {
    Map<String, Object> getAiConfigCheck();

    Map<String, Object> runInventoryAnalysis(AiAnalysisRunRequest request);

    Map<String, Object> listResults(Integer analysisType,
                                    Integer timeGranularity,
                                    Integer pageNum,
                                    Integer pageSize);

    Map<String, Object> getRawResult(String analysisId);
}
