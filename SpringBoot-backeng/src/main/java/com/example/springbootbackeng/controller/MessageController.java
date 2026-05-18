package com.example.springbootbackeng.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.dto.MessageSendRequest;
import com.example.springbootbackeng.dto.MessageView;
import com.example.springbootbackeng.security.SecurityUserContext;
import com.example.springbootbackeng.service.MessageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/messages")
@Validated
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public Result<?> send(@Valid @RequestBody MessageSendRequest request) {
        String senderId = SecurityUserContext.getCurrentUserId();
        if (senderId == null || senderId.trim().isEmpty()) {
            return Result.error("发送人未登录");
        }
        MessageView data = messageService.send(senderId, request);
        if (data == null) {
            return Result.error("发送失败，请检查收件人和内容");
        }
        return Result.success(data);
    }

    @GetMapping("/inbox")
    public Result<?> inbox() {
        String uid = SecurityUserContext.getCurrentUserId();
        uid = uid == null ? "" : uid.trim();
        if (uid.isEmpty()) {
            return Result.error("用户未登录");
        }
        List<MessageView> list = messageService.inbox(uid);
        return Result.success(list);
    }

    @GetMapping("/sent")
    public Result<?> sent() {
        String uid = SecurityUserContext.getCurrentUserId();
        uid = uid == null ? "" : uid.trim();
        if (uid.isEmpty()) {
            return Result.error("用户未登录");
        }
        List<MessageView> list = messageService.sent(uid);
        return Result.success(list);
    }

    @PostMapping("/read")
    public Result<?> markRead(@RequestParam @NotBlank(message = "messageId不能为空") @Size(max = 64, message = "messageId长度不能超过64") String messageId) {
        String userId = SecurityUserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("用户未登录");
        }
        boolean ok = messageService.markRead(userId, messageId);
        return ok ? Result.success("已读") : Result.error("标记失败");
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        String userId = SecurityUserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return new SseEmitter(0L);
        }
        return messageService.subscribe(userId);
    }
}
