package com.example.demo1.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.entity.Beverage;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.mapper.BeverageMapper;
import com.example.demo1.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热门度统计服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PopularityService {
    
    private final UserBehaviorMapper userBehaviorMapper;
    private final BeverageMapper beverageMapper;
    
    /**
     * 计算酒类的热门度分数
     */
    public double calculatePopularityScore(Long beverageId) {
        Beverage beverage = beverageMapper.selectById(beverageId);
        if (beverage == null) {
            return 0.0;
        }
        
        // 获取最近30天的行为数据
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getTargetType, TargetType.BEVERAGE)
                        .eq(UserBehavior::getTargetId, beverageId)
                        .ge(UserBehavior::getCreatedAt, LocalDateTime.now().minusDays(30))
        );
        
        // 计算行为分数
        double behaviorScore = 0.0;
        for (UserBehavior behavior : behaviors) {
            double weight = getBehaviorWeight(behavior.getBehaviorType());
            double timeDecay = calculateTimeDecay(behavior.getCreatedAt());
            behaviorScore += weight * timeDecay;
        }
        
        // 评分分数（归一化到0-1）
        double ratingScore = beverage.getRating() != null 
                ? beverage.getRating().doubleValue() / 5.0 
                : 0.0;
        
        // 浏览数分数（对数归一化）
        double viewScore = beverage.getViewCount() != null 
                ? Math.log1p(beverage.getViewCount()) / 10.0 
                : 0.0;
        
        // 综合分数：行为40% + 评分30% + 浏览30%
        return behaviorScore * 0.4 + ratingScore * 0.3 + Math.min(viewScore, 1.0) * 0.3;
    }
    
    /**
     * 批量计算热门度分数
     */
    public Map<Long, Double> calculatePopularityScores(List<Long> beverageIds) {
        Map<Long, Double> scores = new HashMap<>();
        for (Long beverageId : beverageIds) {
            scores.put(beverageId, calculatePopularityScore(beverageId));
        }
        return scores;
    }
    
    private double getBehaviorWeight(com.example.demo1.common.enums.BehaviorType behaviorType) {
        switch (behaviorType) {
            case VIEW:
                return 1.0;
            case LIKE:
                return 3.0;
            case FAVORITE:
                return 5.0;
            case COMMENT:
                return 4.0;
            case SHARE:
                return 2.0;
            default:
                return 1.0;
        }
    }
    
    private double calculateTimeDecay(LocalDateTime createdAt) {
        long daysAgo = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        return Math.max(0.1, Math.exp(-daysAgo / 30.0));
    }
}



