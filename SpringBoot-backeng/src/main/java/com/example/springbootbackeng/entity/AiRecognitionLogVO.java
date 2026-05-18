package com.example.springbootbackeng.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AiRecognitionLogVO {
    private String recId;
    private String userId;
    private String userName;
    private String originalImageUrl;
    private String aiResultJson;
    private String recognizeMaterialName;
    private Integer recognizeQuantity;
    private BigDecimal confidenceScore;
    private Integer isVerified;
    private String verifyUserId;
    private String verifyUserName;
    private LocalDateTime createTime;
}
