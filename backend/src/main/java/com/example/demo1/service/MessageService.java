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
        
        // 2. 检查接收者的私信接收策略
        checkMessagePolicy(senderId, receiver);
        
        // 3. 检查私信频次限制（未互相关注时，限制最多一条）
        checkMessageFrequencyLimit(senderId, receiverId);
        
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
    private void checkMessagePolicy(Long senderId, User receiver) {
        MessagePolicy policy = receiver.getMessagePolicy();
        if (policy == null) {
            policy = MessagePolicy.ALL; // 默认为接受所有人
        }
        
        switch (policy) {
            case NONE:
                throw new BusinessException("该用户不接受任何私信");
            case FOLLOWERS_ONLY:
                // 检查发送者是否关注了接收者
                boolean isFollowing = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, senderId)
                    .eq(UserFollow::getFolloweeId, receiver.getId())) > 0;
                if (!isFollowing) {
                    throw new BusinessException("该用户仅接受关注者的私信");
                }
                break;
            case ALL:
            default:
                // 允许发送
                break;
        }
    }
    
    /**
     * 检查私信频次限制
     * 在尚未建立互相关注或互动关系前，限制某用户对另一用户的私信条数（如最多一条）
     */
    private void checkMessageFrequencyLimit(Long senderId, Long receiverId) {
        // 检查是否互相关注
        boolean mutualFollow = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
            .eq(UserFollow::getFollowerId, senderId)
            .eq(UserFollow::getFolloweeId, receiverId)) > 0
            && userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
            .eq(UserFollow::getFollowerId, receiverId)
            .eq(UserFollow::getFolloweeId, senderId)) > 0;
        
        // 如果已互相关注，不限制频次
        if (mutualFollow) {
            return;
        }
        
        // 检查是否已有历史对话（接收者回复过发送者）
        boolean hasReceiverReply = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
            .eq(PrivateMessage::getSenderId, receiverId)
            .eq(PrivateMessage::getReceiverId, senderId)) > 0;
        
        // 如果有回复，说明已建立互动关系，不限制频次
        if (hasReceiverReply) {
            return;
        }
        
        // 如果未互相关注且无回复，检查发送者已发送的私信数量
        long sentCount = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
            .eq(PrivateMessage::getSenderId, senderId)
            .eq(PrivateMessage::getReceiverId, receiverId));
        
        // 限制最多一条
        if (sentCount >= 1) {
            throw new BusinessException("在未建立互相关注或互动关系前，您最多只能发送一条私信");
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
