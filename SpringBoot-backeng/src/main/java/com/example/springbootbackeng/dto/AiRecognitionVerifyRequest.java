package com.example.springbootbackeng.dto;

import lombok.Data;

@Data
public class AiRecognitionVerifyRequest {
    private String recognizeMaterialName;
    private Integer recognizeQuantity;
}
