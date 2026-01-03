package com.example.demo1.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.dto.response.RecommendationResult;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐算法
 * 基于用户行为的协同过滤算法，通过找到相似用户来推荐内容
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollaborativeFilteringRecommender {

    private final UserBehaviorMapper userBehaviorMapper;

    // 行为权重配置
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 2.0;
    private static final double FAVORITE_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 1.5;
    private static final double SHARE_WEIGHT = 2.0;

    /**
     * 生成协同过滤推荐列表
     * 
     * @param userId 用户ID
     * @param candidatePosts 候选动态列表
     * @param limit 返回数量限制
     * @return 推荐结果列表
     */
    public List<RecommendationResult> recommend(Long userId, List<SharePost> candidatePosts, int limit) {
        // 1. 找到相似用户(Top-K)
        List<Long> similarUserIds = findSimilarUsers(userId, 10);
        
        if (similarUserIds.isEmpty()) {
            log.debug("用户{}没有找到相似用户，无法进行协同过滤推荐", userId);
            return Collections.emptyList();
        }

        // 2. 获取相似用户喜欢的动态
        Set<Long> similarUserLikedPostIds = getSimilarUserLikedPosts(similarUserIds, userId);
        
        if (similarUserLikedPostIds.isEmpty()) {
            log.debug("相似用户没有喜欢的动态", userId);
            return Collections.emptyList();
        }

        // 3. 计算用户相似度映射
        Map<Long, Double> userSimilarityMap = calculateUserSimilarities(userId, similarUserIds);

        // 4. 过滤候选动态，只保留相似用户喜欢的
        // 5. 按相似度加权排序
        List<RecommendationResult> results = candidatePosts.stream()
            .filter(post -> similarUserLikedPostIds.contains(post.getId()))
            .map(post -> {
                double score = calculateCollaborativeScore(post, similarUserIds, userSimilarityMap);
                return new RecommendationResult(post.getId(), score, "相似用户喜欢");
            })
            .filter(r -> r.getScore() > 0)
            .sorted(Comparator.comparingDouble(RecommendationResult::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        log.debug("协同过滤推荐: 用户{}, 相似用户数{}, 推荐数{}", userId, similarUserIds.size(), results.size());
        return results;
    }

    /**
     * 查找相似用户（Top-K）
     * 
     * @param userId 用户ID
     * @param limit 返回的相似用户数量
     * @return 相似用户ID列表
     */
    public List<Long> findSimilarUsers(Long userId, int limit) {
        // 获取所有有行为的用户ID（排除当前用户）
        Set<Long> allUserIds = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .ge(UserBehavior::getCreatedAt, LocalDateTime.now().minusDays(30))
                .select(UserBehavior::getUserId)
        ).stream()
        .map(UserBehavior::getUserId)
        .filter(id -> !id.equals(userId))
        .collect(Collectors.toSet());

        if (allUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算与所有用户的相似度
        List<UserSimilarity> similarities = allUserIds.stream()
            .map(otherUserId -> {
                double similarity = calculateUserSimilarity(userId, otherUserId);
                return new UserSimilarity(otherUserId, similarity);
            })
            .filter(us -> us.similarity > 0)
            .sorted(Comparator.comparingDouble((UserSimilarity us) -> us.similarity).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return similarities.stream()
            .map(us -> us.userId)
            .collect(Collectors.toList());
    }

    /**
     * 计算用户相似度（余弦相似度）
     * 
     * @param user1Id 用户1 ID
     * @param user2Id 用户2 ID
     * @return 相似度分数（0-1之间）
     */
    public double calculateUserSimilarity(Long user1Id, Long user2Id) {
        // 获取用户互动向量
        Map<Long, Double> vector1 = getUserInteractionVector(user1Id);
        Map<Long, Double> vector2 = getUserInteractionVector(user2Id);

        if (vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        // 计算余弦相似度
        Set<Long> commonPostIds = new HashSet<>(vector1.keySet());
        commonPostIds.retainAll(vector2.keySet());

        if (commonPostIds.isEmpty()) {
            return 0.0;
        }

        // 计算点积
        double dotProduct = 0.0;
        for (Long postId : commonPostIds) {
            double v1 = vector1.get(postId);
            double v2 = vector2.get(postId);
            dotProduct += v1 * v2;
        }

        // 计算向量范数
        double norm1 = 0.0;
        for (Double value : vector1.values()) {
            norm1 += value * value;
        }
        norm1 = Math.sqrt(norm1);

        double norm2 = 0.0;
        for (Double value : vector2.values()) {
            norm2 += value * value;
        }
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * 获取用户互动向量
     * 浏览: 权重1.0, 点赞: 权重2.0, 收藏: 权重3.0, 评论: 权重1.5
     * 
     * @param userId 用户ID
     * @return 动态ID -> 互动权重的映射
     */
    private Map<Long, Double> getUserInteractionVector(Long userId) {
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .ge(UserBehavior::getCreatedAt, LocalDateTime.now().minusDays(30))
        );

        Map<Long, Double> vector = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            Long postId = behavior.getTargetId();
            double weight = getBehaviorWeight(behavior.getBehaviorType());
            vector.put(postId, vector.getOrDefault(postId, 0.0) + weight);
        }

        return vector;
    }

    /**
     * 获取行为权重
     */
    private double getBehaviorWeight(BehaviorType behaviorType) {
        switch (behaviorType) {
            case VIEW:
                return VIEW_WEIGHT;
            case LIKE:
                return LIKE_WEIGHT;
            case FAVORITE:
                return FAVORITE_WEIGHT;
            case COMMENT:
                return COMMENT_WEIGHT;
            case SHARE:
                return SHARE_WEIGHT;
            default:
                return 1.0;
        }
    }

    /**
     * 计算用户相似度映射
     */
    private Map<Long, Double> calculateUserSimilarities(Long userId, List<Long> similarUserIds) {
        Map<Long, Double> similarityMap = new HashMap<>();
        for (Long similarUserId : similarUserIds) {
            double similarity = calculateUserSimilarity(userId, similarUserId);
            similarityMap.put(similarUserId, similarity);
        }
        return similarityMap;
    }

    /**
     * 获取相似用户喜欢的动态
     * 过滤掉当前用户已看过的动态
     */
    private Set<Long> getSimilarUserLikedPosts(List<Long> similarUserIds, Long currentUserId) {
        // 获取当前用户已看过的动态
        Set<Long> currentUserViewedPosts = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, currentUserId)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .select(UserBehavior::getTargetId)
        ).stream()
        .map(UserBehavior::getTargetId)
        .collect(Collectors.toSet());

        // 获取相似用户喜欢的动态（点赞、收藏、评论）
        Set<Long> likedPostIds = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .in(UserBehavior::getUserId, similarUserIds)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .in(UserBehavior::getBehaviorType, 
                    Arrays.asList(BehaviorType.LIKE, BehaviorType.FAVORITE, BehaviorType.COMMENT))
        ).stream()
        .map(UserBehavior::getTargetId)
        .filter(postId -> !currentUserViewedPosts.contains(postId))
        .collect(Collectors.toSet());

        return likedPostIds;
    }

    /**
     * 计算协同过滤推荐分数
     * 按相似度加权求和相似用户对该动态的行为权重
     */
    private double calculateCollaborativeScore(SharePost post, List<Long> similarUserIds, Map<Long, Double> userSimilarityMap) {
        // 统计有多少相似用户喜欢这个动态
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .in(UserBehavior::getUserId, similarUserIds)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .eq(UserBehavior::getTargetId, post.getId())
                .in(UserBehavior::getBehaviorType,
                    Arrays.asList(BehaviorType.LIKE, BehaviorType.FAVORITE, BehaviorType.COMMENT))
        );

        // 按相似度加权求和
        double score = 0.0;
        for (UserBehavior behavior : behaviors) {
            Long similarUserId = behavior.getUserId();
            double similarity = userSimilarityMap.getOrDefault(similarUserId, 0.0);
            double behaviorWeight = getBehaviorWeight(behavior.getBehaviorType());
            score += similarity * behaviorWeight;
        }

        return score;
    }

    /**
     * 用户相似度内部类
     */
    private static class UserSimilarity {
        final Long userId;
        final double similarity;

        UserSimilarity(Long userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
    }
}

