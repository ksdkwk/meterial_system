package com.example.springbootbackeng.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.springbootbackeng.dto.MessageSendRequest;
import com.example.springbootbackeng.dto.MessageView;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.mapper.UserMapper;
import com.example.springbootbackeng.service.MessageService;

@Service
public class MessageServiceImpl implements MessageService {

    private final UserMapper userMapper;

    public MessageServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    private static class MessageRecord {
        private String messageId;
        private String senderId;
        private String senderName;
        private String receiverId;
        private String receiverName;
        private String subject;
        private String content;
        private LocalDateTime sendTime;
        private boolean read;
    }

    private final Map<String, MessageRecord> messageStore = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<String>> inboxIndex = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<String>> sentIndex = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    @Override
    public MessageView send(String senderId, MessageSendRequest request) {
        if (senderId == null || senderId.trim().isEmpty() || request == null) {
            return null;
        }
        String receiverInput = request.getReceiver() == null ? "" : request.getReceiver().trim();
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (receiverInput.isEmpty() || content.isEmpty()) {
            return null;
        }

        User sender = userMapper.selectById(senderId.trim());
        if (sender == null) {
            return null;
        }

        User receiver = userMapper.selectById(receiverInput);
        if (receiver == null) {
            receiver = userMapper.selectByUsernameAndStatus(receiverInput, 1);
        }
        if (receiver == null) {
            return null;
        }

        MessageRecord rec = new MessageRecord();
        rec.messageId = UUID.randomUUID().toString().replace("-", "");
        rec.senderId = sender.getUserId();
        rec.senderName = sender.getRealName() == null ? sender.getUsername() : sender.getRealName();
        rec.receiverId = receiver.getUserId();
        rec.receiverName = receiver.getRealName() == null ? receiver.getUsername() : receiver.getRealName();
        rec.subject = request.getSubject() == null ? "" : request.getSubject().trim();
        rec.content = content;
        rec.sendTime = LocalDateTime.now();
        rec.read = false;

        messageStore.put(rec.messageId, rec);
        inboxIndex.computeIfAbsent(rec.receiverId, k -> new CopyOnWriteArrayList<>()).add(rec.messageId);
        sentIndex.computeIfAbsent(rec.senderId, k -> new CopyOnWriteArrayList<>()).add(rec.messageId);

        MessageView view = toView(rec);
        pushToUser(rec.receiverId, view);
        if (!rec.receiverId.equals(rec.senderId)) {
            pushToUser(rec.senderId, view);
        }
        return view;
    }

    @Override
    public List<MessageView> inbox(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return List.of();
        }
        List<MessageView> list = new ArrayList<>();
        for (String id : inboxIndex.getOrDefault(userId.trim(), new CopyOnWriteArrayList<>())) {
            MessageRecord rec = messageStore.get(id);
            if (rec != null) {
                list.add(toView(rec));
            }
        }
        list.sort(Comparator.comparing(MessageView::getSendTime).reversed());
        return list;
    }

    @Override
    public List<MessageView> sent(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return List.of();
        }
        List<MessageView> list = new ArrayList<>();
        for (String id : sentIndex.getOrDefault(userId.trim(), new CopyOnWriteArrayList<>())) {
            MessageRecord rec = messageStore.get(id);
            if (rec != null) {
                list.add(toView(rec));
            }
        }
        list.sort(Comparator.comparing(MessageView::getSendTime).reversed());
        return list;
    }

    @Override
    public boolean markRead(String userId, String messageId) {
        if (userId == null || messageId == null) {
            return false;
        }
        MessageRecord rec = messageStore.get(messageId);
        if (rec == null) {
            return false;
        }
        if (!userId.trim().equals(rec.receiverId)) {
            return false;
        }
        rec.read = true;
        return true;
    }

    @Override
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(0L);
        if (userId == null || userId.trim().isEmpty()) {
            emitter.complete();
            return emitter;
        }
        String key = userId.trim();
        subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError((e) -> removeEmitter(key, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            removeEmitter(key, emitter);
        }
        return emitter;
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = subscribers.get(userId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    private void pushToUser(String userId, MessageView view) {
        CopyOnWriteArrayList<SseEmitter> list = subscribers.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        List<SseEmitter> bad = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("message").data(view));
            } catch (IOException e) {
                bad.add(emitter);
            }
        }
        list.removeAll(bad);
    }

    private MessageView toView(MessageRecord rec) {
        MessageView v = new MessageView();
        v.setMessageId(rec.messageId);
        v.setSenderId(rec.senderId);
        v.setSenderName(rec.senderName);
        v.setReceiverId(rec.receiverId);
        v.setReceiverName(rec.receiverName);
        v.setSubject(rec.subject);
        v.setContent(rec.content);
        v.setSendTime(rec.sendTime);
        v.setRead(rec.read);
        return v;
    }
}
