package com.example.demo1.service;

import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户行为服务
 * 记录用户行为用于推荐系统
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorService {

    private final UserBehaviorMapper userBehaviorMapper;

    /**
     * 记录用户行为
     * 
     * @param userId 用户ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param behaviorType 行为类型
     */
    @Transactional
    public void recordBehavior(Long userId, TargetType targetType, Long targetId, BehaviorType behaviorType) {
        if (userId == null || targetId == null) {
            return;
        }
        
        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setTargetType(targetType);
            behavior.setTargetId(targetId);
            behavior.setBehaviorType(behaviorType);
            behavior.setWeight(behaviorType.getWeight());
            
            userBehaviorMapper.insert(behavior);
            
            log.debug("记录用户行为: userId={}, targetType={}, targetId={}, behaviorType={}, weight={}", 
                userId, targetType, targetId, behaviorType, behaviorType.getWeight());
        } catch (Exception e) {
            // 行为记录失败不应该影响主流程
            log.warn("记录用户行为失败", e);
        }
    }
}

