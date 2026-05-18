package com.example.springbootbackeng.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_ai_analysis_result")
public class AiAnalysisResult {

    @TableId(value = "analysis_id", type = IdType.INPUT)
    private String analysisId;

    private Integer analysisType;
    private Integer stockoutCalcType;
    private Integer timeGranularity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String materialId;
    private String materialType;
    private String storageLocation;

    @TableField("analysis_result")
    private String analysisResult;

    private BigDecimal coreIndicator;
    private String createUserId;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private Integer analysisStatus;
    private String failReason;
}
