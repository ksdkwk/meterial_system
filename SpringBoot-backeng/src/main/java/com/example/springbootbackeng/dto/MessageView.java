package com.example.springbootbackeng.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MessageView {
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String subject;
    private String content;
    private LocalDateTime sendTime;
    private Boolean read;
}
