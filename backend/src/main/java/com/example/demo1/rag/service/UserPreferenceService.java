package com.example.demo1.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.config.RAGConfig;
import com.example.demo1.dto.rag.UserPreferenceVO;
import com.example.demo1.entity.Beverage;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.mapper.BeverageMapper;
import com.example.demo1.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户偏好提取服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {
    
    private final UserBehaviorMapper userBehaviorMapper;
    private final BeverageMapper beverageMapper;
    private final RAGConfig ragConfig;
    private final EmbeddingService embeddingService;
    
    /**
     * 提取用户偏好
     */
    public UserPreferenceVO extractUserPreference(Long userId) {
        if (userId == null) {
            return null;
        }
        
        // 获取用户对酒类的行为数据（最近90天）
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getTargetType, TargetType.BEVERAGE)
                        .ge(UserBehavior::getCreatedAt, LocalDateTime.now().minusDays(90))
        );
        
        if (behaviors.isEmpty()) {
            return null;
        }
        
        // 获取相关的酒类信息
        Set<Long> beverageIds = behaviors.stream()
                .map(UserBehavior::getTargetId)
                .collect(Collectors.toSet());
        
        List<Beverage> beverages = beverageMapper.selectBatchIds(beverageIds);
        Map<Long, Beverage> beverageMap = beverages.stream()
                .collect(Collectors.toMap(Beverage::getId, b -> b));
        
        // 计算类型偏好
        Map<String, Double> typePreference = calculateTypePreference(behaviors, beverageMap);
        
        // 计算口味偏好
        Map<String, Double> tastePreference = calculateTastePreference(behaviors, beverageMap);
        
        // 计算产地偏好
        Map<String, Double> originPreference = calculateOriginPreference(behaviors, beverageMap);
        
        // 构建偏好向量
        List<Float> preferenceVector = buildPreferenceVector(typePreference, tastePreference, originPreference);
        
        return UserPreferenceVO.builder()
                .userId(userId)
                .typePreference(typePreference)
                .tastePreference(tastePreference)
                .originPreference(originPreference)
                .preferenceVector(preferenceVector)
                .build();
    }
    
    /**
     * 计算类型偏好
     */
    private Map<String, Double> calculateTypePreference(List<UserBehavior> behaviors, Map<Long, Beverage> beverageMap) {
        Map<String, Double> typeScores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            Beverage beverage = beverageMap.get(behavior.getTargetId());
            if (beverage != null && beverage.getType() != null) {
                double weight = getBehaviorWeight(behavior.getBehaviorType());
                double timeDecay = calculateTimeDecay(behavior.getCreatedAt());
                double score = weight * timeDecay;
                
                typeScores.put(beverage.getType(),
                        typeScores.getOrDefault(beverage.getType(), 0.0) + score);
            }
        }
        
        // 归一化
        return normalizeScores(typeScores);
    }
    
    /**
     * 计算口味偏好
     */
    private Map<String, Double> calculateTastePreference(List<UserBehavior> behaviors, Map<Long, Beverage> beverageMap) {
        Map<String, Double> tasteScores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            Beverage beverage = beverageMap.get(behavior.getTargetId());
            if (beverage != null && beverage.getTasteNotes() != null) {
                double weight = getBehaviorWeight(behavior.getBehaviorType());
                double timeDecay = calculateTimeDecay(behavior.getCreatedAt());
                double score = weight * timeDecay;
                
                // 提取关键词（简单分词）
                String[] keywords = extractKeywords(beverage.getTasteNotes());
                for (String keyword : keywords) {
                    if (keyword.length() > 1) {
                        tasteScores.put(keyword,
                                tasteScores.getOrDefault(keyword, 0.0) + score);
                    }
                }
            }
        }
        
        return normalizeScores(tasteScores);
    }
    
    /**
     * 计算产地偏好
     */
    private Map<String, Double> calculateOriginPreference(List<UserBehavior> behaviors, Map<Long, Beverage> beverageMap) {
        Map<String, Double> originScores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            Beverage beverage = beverageMap.get(behavior.getTargetId());
            if (beverage != null && beverage.getOrigin() != null) {
                double weight = getBehaviorWeight(behavior.getBehaviorType());
                double timeDecay = calculateTimeDecay(behavior.getCreatedAt());
                double score = weight * timeDecay;
                
                originScores.put(beverage.getOrigin(),
                        originScores.getOrDefault(beverage.getOrigin(), 0.0) + score);
            }
        }
        
        return normalizeScores(originScores);
    }
    
    /**
     * 提取关键词（简单实现）
     */
    private String[] extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        
        // 移除标点符号，按空格和常见分隔符分割
        String cleaned = text.replaceAll("[，,。.、；;：:！!？?]", " ");
        return cleaned.split("\\s+");
    }
    
    /**
     * 计算时间衰减因子
     */
    private double calculateTimeDecay(LocalDateTime createdAt) {
        long daysAgo = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        // 指数衰减：30天内权重1.0，之后逐渐衰减
        return Math.max(0.1, Math.exp(-daysAgo / 30.0));
    }
    
    /**
     * 获取行为权重
     */
    private double getBehaviorWeight(BehaviorType behaviorType) {
        RAGConfig.BehaviorWeight weights = ragConfig.getBehaviorWeight();
        switch (behaviorType) {
            case VIEW:
                return weights.getView();
            case LIKE:
                return weights.getLike();
            case FAVORITE:
                return weights.getFavorite();
            case COMMENT:
                return weights.getComment();
            case SHARE:
                return weights.getShare();
            default:
                return 1.0;
        }
    }
    
    /**
     * 归一化分数
     */
    private Map<String, Double> normalizeScores(Map<String, Double> scores) {
        if (scores.isEmpty()) {
            return scores;
        }
        
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxScore == 0) {
            return scores;
        }
        
        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / maxScore);
        }
        
        return normalized;
    }
    
    /**
     * 构建偏好向量（用于向量检索）
     */
    private List<Float> buildPreferenceVector(Map<String, Double> typePreference,
                                                Map<String, Double> tastePreference,
                                                Map<String, Double> originPreference) {
        // 简化实现：将偏好信息拼接成文本，然后向量化
        StringBuilder text = new StringBuilder();
        
        // 类型偏好
        typePreference.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> text.append(e.getKey()).append(" "));
        
        // 口味偏好
        tastePreference.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> text.append(e.getKey()).append(" "));
        
        // 产地偏好
        originPreference.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> text.append(e.getKey()).append(" "));
        
        // 向量化
        return embeddingService.embedText(text.toString());
    }
}



