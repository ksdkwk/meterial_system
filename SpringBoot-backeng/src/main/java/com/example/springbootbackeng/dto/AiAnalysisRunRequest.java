package com.example.springbootbackeng.dto;

import lombok.Data;

@Data
public class AiAnalysisRunRequest {
    private Integer timeGranularity;
    private String startTime;
    private String endTime;
    private String materialId;
    private String materialType;
    private String storageLocation;
    private Integer stockoutCalcType;
    private String createUserId;
}
