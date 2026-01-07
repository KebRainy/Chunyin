package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.common.enums.MessagePolicy;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.dto.request.SendMessageRequest;
import com.example.demo1.dto.response.ConversationSummaryVO;
import com.example.demo1.dto.response.PrivateMessageVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.PrivateMessage;
import com.example.demo1.entity.User;
import com.example.demo1.entity.UserBlock;
import com.example.demo1.entity.UserFollow;
import com.example.demo1.mapper.PrivateMessageMapper;
import com.example.demo1.mapper.UserBlockMapper;
import com.example.demo1.mapper.UserFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserBlockMapper userBlockMapper;
    private final UserFollowMapper userFollowMapper;

    @Transactional
    public PrivateMessageVO sendMessage(Long senderId, Long receiverId, SendMessageRequest request) {
        // 检查接收者是否存在
        User receiver = userService.getRequiredUser(receiverId);
        User sender = userService.getUserById(senderId);
        
        // 1. 检查是否被屏蔽
        if (isBlocked(senderId, receiverId)) {
            throw new BusinessException("您已被该用户屏蔽，无法发送私信");
        }
        
        // 2. 检查接收者的私信接收策略（含频次限制）
        checkMessagePolicy(senderId, receiverId, receiver);
        
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(request.getContent());
        privateMessageMapper.insert(message);

        return toVo(message, sender, receiver, senderId);
    }
    
    /**
     * 检查发送者是否被接收者屏蔽
     */
    private boolean isBlocked(Long senderId, Long receiverId) {
        return userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
            .eq(UserBlock::getBlockerId, receiverId)
            .eq(UserBlock::getBlockedId, senderId)) > 0;
    }
    
    /**
     * 检查接收者的私信接收策略
     */
    private void checkMessagePolicy(Long senderId, Long receiverId, User receiver) {
        MessagePolicy policy = receiver.getMessagePolicy();
        if (policy == null) {
            policy = MessagePolicy.LIMIT_ONE_BEFORE_REPLY_OR_FOLLOW; // 默认为限制一条
        }
        
        switch (policy) {
            case NONE:
                throw new BusinessException("该用户不接受任何私信");
            case FOLLOWEES_ONLY: {
                // 仅我关注的人：接收者必须关注发送者
                boolean receiverFollowsSender = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, receiverId)
                    .eq(UserFollow::getFolloweeId, senderId)) > 0;
                if (!receiverFollowsSender) {
                    throw new BusinessException("该用户仅接受其关注对象的私信");
                }
                return;
            }
            case LIMIT_ONE_BEFORE_REPLY_OR_FOLLOW: {
                // 如果接收者已关注发送者，或已回复过，则不限制
                boolean receiverFollowsSender = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, receiverId)
                    .eq(UserFollow::getFolloweeId, senderId)) > 0;
                if (receiverFollowsSender) {
                    return;
                }

                boolean hasReceiverReply = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getSenderId, receiverId)
                    .eq(PrivateMessage::getReceiverId, senderId)) > 0;
                if (hasReceiverReply) {
                    return;
                }

                long sentCount = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getSenderId, senderId)
                    .eq(PrivateMessage::getReceiverId, receiverId));
                if (sentCount >= 1) {
                    throw new BusinessException("在对方回复或关注你前，你最多只能发送一条私信");
                }
                return;
            }
            case FOLLOWERS_ONLY: {
                // 兼容旧值：Ta关注我：发送者必须关注接收者
                boolean senderFollowsReceiver = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, senderId)
                    .eq(UserFollow::getFolloweeId, receiverId)) > 0;
                if (!senderFollowsReceiver) {
                    throw new BusinessException("该用户仅接受关注者的私信");
                }
                return;
            }
            case ALL:
            default:
                // 允许发送
                return;
        }
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
