package com.example.springbootbackeng.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_ai_recognition_log")
public class AiRecognitionLog {

    @TableId(value = "rec_id", type = IdType.INPUT)
    private String recId;

    private String userId;
    private String originalImageUrl;
    private String aiResultJson;
    private String recognizeMaterialName;
    private Integer recognizeQuantity;
    private BigDecimal confidenceScore;
    private Integer isVerified;
    private String verifyUserId;
    private LocalDateTime createTime;
}
