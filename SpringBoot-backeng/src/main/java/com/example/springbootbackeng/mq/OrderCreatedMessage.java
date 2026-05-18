package com.example.springbootbackeng.mq;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderCreatedMessage {
    private String demandId;
    private String demandUserId;
    private String materialId;
    private Integer demandQuantity;
    private LocalDateTime useTime;
    private LocalDateTime createdAt;
}
