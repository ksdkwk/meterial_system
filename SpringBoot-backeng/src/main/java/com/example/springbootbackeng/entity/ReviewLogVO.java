package com.example.springbootbackeng.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReviewLogVO {
    private String reviewId;
    private String reviewDemandId;
    private String reviewApplyId;
    private String reviewApplyName;
    private String reviewApproveId;
    private String reviewApproveName;
    private String reviewState;
    private LocalDateTime reviewTime;
    private String reviewResult;
    private String reviewReason;
    private String reviewOther;
}
