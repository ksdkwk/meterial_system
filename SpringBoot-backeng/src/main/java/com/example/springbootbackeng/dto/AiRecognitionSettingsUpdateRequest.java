package com.example.springbootbackeng.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AiRecognitionSettingsUpdateRequest {
    private Integer baikeNum;
    private BigDecimal minConfidenceThreshold;
    private Map<String, List<String>> keywordDict;

    public Integer getBaikeNum() {
        return baikeNum;
    }

    public void setBaikeNum(Integer baikeNum) {
        this.baikeNum = baikeNum;
    }

    public BigDecimal getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }

    public void setMinConfidenceThreshold(BigDecimal minConfidenceThreshold) {
        this.minConfidenceThreshold = minConfidenceThreshold;
    }

    public Map<String, List<String>> getKeywordDict() {
        return keywordDict;
    }

    public void setKeywordDict(Map<String, List<String>> keywordDict) {
        this.keywordDict = keywordDict;
    }
}
