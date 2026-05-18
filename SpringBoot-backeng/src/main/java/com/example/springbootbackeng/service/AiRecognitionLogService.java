package com.example.springbootbackeng.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.springbootbackeng.entity.AiRecognitionLogVO;

public interface AiRecognitionLogService {
    List<AiRecognitionLogVO> listVisibleLogs(String keyword, String currentUserId, boolean isManager);

    Map<String, Object> runRecognition(String userId, String originalImageUrl);

    String verifyRecognition(String recId, String verifyUserId, String recognizeMaterialName, Integer recognizeQuantity);

    Map<String, Object> getAiConfigCheck();

    Map<String, Object> getRecognitionSettings();

    String updateRecognitionSettings(String operatorUserId,
                                     Integer baikeNum,
                                     BigDecimal minConfidenceThreshold,
                                     Map<String, List<String>> keywordDict);
}
