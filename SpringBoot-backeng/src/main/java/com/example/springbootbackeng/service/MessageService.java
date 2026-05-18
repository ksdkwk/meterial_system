package com.example.springbootbackeng.service;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.springbootbackeng.dto.MessageSendRequest;
import com.example.springbootbackeng.dto.MessageView;

public interface MessageService {
    MessageView send(String senderId, MessageSendRequest request);

    List<MessageView> inbox(String userId);

    List<MessageView> sent(String userId);

    boolean markRead(String userId, String messageId);

    SseEmitter subscribe(String userId);
}
