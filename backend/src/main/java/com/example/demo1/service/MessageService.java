package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.dto.request.SendMessageRequest;
import com.example.demo1.dto.response.ConversationSummaryVO;
import com.example.demo1.dto.response.PrivateMessageVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.PrivateMessage;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.PrivateMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final UserService userService;

    @Transactional
    public PrivateMessageVO sendMessage(Long senderId, Long receiverId, SendMessageRequest request) {
        userService.getRequiredUser(receiverId);
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(request.getContent());
        privateMessageMapper.insert(message);

        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(receiverId);
        return toVo(message, sender, receiver, senderId);
    }

    public List<PrivateMessageVO> listConversation(Long userId, Long peerId) {
        List<PrivateMessage> messages = privateMessageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
            .and(wrapper -> wrapper
                .eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, peerId)
                .or()
                .eq(PrivateMessage::getSenderId, peerId).eq(PrivateMessage::getReceiverId, userId))
            .orderByAsc(PrivateMessage::getCreatedAt));
        // mark as read
        privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
            .eq(PrivateMessage::getSenderId, peerId)
            .eq(PrivateMessage::getReceiverId, userId)
            .eq(PrivateMessage::getIsRead, false)
            .set(PrivateMessage::getIsRead, true));
        User me = userService.getUserById(userId);
        User peer = userService.getUserById(peerId);
        return messages.stream()
            .map(message -> {
                User sender = message.getSenderId().equals(userId) ? me : peer;
                User receiver = message.getSenderId().equals(userId) ? peer : me;
                return toVo(message, sender, receiver, userId);
            })
            .collect(Collectors.toList());
    }

    public List<ConversationSummaryVO> listConversations(Long userId) {
        List<PrivateMessage> latestMessages = privateMessageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
            .and(wrapper -> wrapper.eq(PrivateMessage::getSenderId, userId)
                .or()
                .eq(PrivateMessage::getReceiverId, userId))
            .orderByDesc(PrivateMessage::getCreatedAt)
            .last("limit 100"));
        Map<Long, PrivateMessage> byPeer = new LinkedHashMap<>();
        for (PrivateMessage message : latestMessages) {
            Long peerId = message.getSenderId().equals(userId) ? message.getReceiverId() : message.getSenderId();
            byPeer.putIfAbsent(peerId, message);
        }
        if (byPeer.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, User> peerMap = userService.mapByIds(new ArrayList<>(byPeer.keySet()));
        List<ConversationSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<Long, PrivateMessage> entry : byPeer.entrySet()) {
            Long peerId = entry.getKey();
            PrivateMessage message = entry.getValue();
            long unread = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getSenderId, peerId)
                .eq(PrivateMessage::getReceiverId, userId)
                .eq(PrivateMessage::getIsRead, false));
            User peer = peerMap.get(peerId);
            SimpleUserVO peerVo = userService.buildSimpleUser(peer);
            summaries.add(ConversationSummaryVO.builder()
                .peer(peerVo)
                .lastMessage(message.getContent())
                .lastMessageTime(message.getCreatedAt())
                .unreadCount(unread)
                .build());
        }
        return summaries;
    }

    private PrivateMessageVO toVo(PrivateMessage message, User sender, User receiver, Long currentUserId) {
        SimpleUserVO senderVo = userService.buildSimpleUser(sender);
        SimpleUserVO receiverVo = userService.buildSimpleUser(receiver);
        return PrivateMessageVO.builder()
            .id(message.getId())
            .sender(senderVo)
            .receiver(receiverVo)
            .content(message.getContent())
            .mine(message.getSenderId().equals(currentUserId))
            .createdAt(message.getCreatedAt())
            .build();
    }
}
