package com.example.demo1.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.dto.response.RecommendationResult;
import com.example.demo1.entity.PostTag;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.mapper.PostTagMapper;
import com.example.demo1.mapper.SharePostMapper;
import com.example.demo1.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内容的推荐算法
 * 基于用户偏好特征(标签、地点)与动态内容的相似度进行推荐
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentBasedRecommender {

    private final UserBehaviorMapper userBehaviorMapper;
    private final PostTagMapper postTagMapper;
    private final SharePostMapper sharePostMapper;

    // 行为权重配置
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 2.0;
    private static final double FAVORITE_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 1.5;
    private static final double SHARE_WEIGHT = 2.0;

    // 标签和地点权重配置
    private static final double TAG_WEIGHT = 0.7;  // 标签权重
    private static final double LOCATION_WEIGHT = 0.3;  // 地点权重

    /**
     * 生成基于内容的推荐列表
     * 
     * @param userId 用户ID
     * @param candidatePosts 候选动态列表
     * @param limit 返回数量限制
     * @return 推荐结果列表
     */
    public List<RecommendationResult> recommend(Long userId, List<SharePost> candidatePosts, int limit) {
        // 1. 构建用户偏好画像
        UserProfile userProfile = buildUserProfile(userId);
        
        if (userProfile.isEmpty()) {
            log.debug("用户{}没有足够的偏好数据，无法进行基于内容的推荐", userId);
            return Collections.emptyList();
        }

        // 2. 计算候选动态与用户偏好的相似度
        List<RecommendationResult> results = candidatePosts.stream()
            .map(post -> {
                double similarity = calculateSimilarity(userProfile, post);
                return new RecommendationResult(post.getId(), similarity, "内容匹配");
            })
            .filter(r -> r.getScore() > 0)
            .sorted(Comparator.comparingDouble(RecommendationResult::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        log.debug("基于内容推荐: 用户{}, 候选数{}, 推荐数{}", userId, candidatePosts.size(), results.size());
        return results;
    }

    /**
     * 计算用户偏好与动态的相似度
     * 综合标签相似度和地点匹配度
     */
    private double calculateSimilarity(UserProfile userProfile, SharePost post) {
        // 计算标签相似度
        double tagScore = calculateTagScore(userProfile, post);
        
        // 计算地点匹配分数
        double locationScore = calculateLocationScore(userProfile, post);
        
        // 综合分数：标签权重 * 标签分数 + 地点权重 * 地点分数
        return TAG_WEIGHT * tagScore + LOCATION_WEIGHT * locationScore;
    }

    /**
     * 计算标签匹配分数（余弦相似度）
     */
    private double calculateTagScore(UserProfile userProfile, SharePost post) {
        // 获取动态的标签
        List<PostTag> postTags = postTagMapper.selectList(
            new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, post.getId())
        );

        if (postTags.isEmpty() || userProfile.tagWeights.isEmpty()) {
            return 0.0;
        }

        // 构建动态标签向量（每个标签权重相等）
        Map<String, Double> postTagWeights = postTags.stream()
            .collect(Collectors.toMap(
                PostTag::getTagName,
                tag -> 1.0 / postTags.size(),  // 每个标签权重相等
                (v1, v2) -> v1
            ));

        // 计算余弦相似度
        return calculateCosineSimilarity(userProfile.tagWeights, postTagWeights);
    }

    /**
     * 计算地点匹配分数
     */
    private double calculateLocationScore(UserProfile userProfile, SharePost post) {
        if (post.getLocation() == null || post.getLocation().isEmpty()) {
            return 0.0;
        }

        String postLocation = post.getLocation().trim();
        
        // 查找用户对该地点的偏好权重
        Double locationWeight = userProfile.locationWeights.get(postLocation);
        
        if (locationWeight == null || locationWeight == 0.0) {
            return 0.0;
        }

        return locationWeight;
    }

    /**
     * 计算余弦相似度
     * similarity = Σ(userWeight[tag] * postWeight[tag]) / (||userVector|| * ||postVector||)
     */
    private double calculateCosineSimilarity(Map<String, Double> userWeights, Map<String, Double> postWeights) {
        // 计算点积
        double dotProduct = 0.0;
        Set<String> commonTags = new HashSet<>(userWeights.keySet());
        commonTags.retainAll(postWeights.keySet());

        for (String tag : commonTags) {
            dotProduct += userWeights.get(tag) * postWeights.get(tag);
        }

        // 计算用户向量范数
        double userNorm = 0.0;
        for (Double weight : userWeights.values()) {
            userNorm += weight * weight;
        }
        userNorm = Math.sqrt(userNorm);

        // 计算动态向量范数
        double postNorm = 0.0;
        for (Double weight : postWeights.values()) {
            postNorm += weight * weight;
        }
        postNorm = Math.sqrt(postNorm);

        if (userNorm == 0.0 || postNorm == 0.0) {
            return 0.0;
        }

        return dotProduct / (userNorm * postNorm);
    }

    /**
     * 构建用户偏好画像
     */
    private UserProfile buildUserProfile(Long userId) {
        // 获取用户最近30天的行为数据
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .ge(UserBehavior::getCreatedAt, LocalDateTime.now().minusDays(30))
        );

        if (behaviors.isEmpty()) {
            return new UserProfile();
        }

        // 获取用户互动过的动态ID
        Set<Long> postIds = behaviors.stream()
            .map(UserBehavior::getTargetId)
            .collect(Collectors.toSet());

        // 获取这些动态的标签
        List<PostTag> tags = postTagMapper.selectList(
            new LambdaQueryWrapper<PostTag>()
                .in(PostTag::getPostId, postIds)
        );

        // 计算每个动态的行为权重
        Map<Long, Double> postBehaviorWeights = behaviors.stream()
            .collect(Collectors.toMap(
                UserBehavior::getTargetId,
                b -> getBehaviorWeight(b.getBehaviorType()),
                Double::sum
            ));

        // 构建标签权重映射
        Map<String, Double> tagWeights = new HashMap<>();
        for (PostTag tag : tags) {
            Long postId = tag.getPostId();
            double behaviorWeight = postBehaviorWeights.getOrDefault(postId, 1.0);
            String tagName = tag.getTagName();
            tagWeights.put(tagName, tagWeights.getOrDefault(tagName, 0.0) + behaviorWeight);
        }

        // 构建地点权重映射
        Map<String, Double> locationWeights = new HashMap<>();
        // 批量查询用户互动过的动态，获取地点信息
        List<SharePost> interactedPosts = sharePostMapper.selectBatchIds(postIds);
        Map<Long, SharePost> postMap = interactedPosts.stream()
            .collect(Collectors.toMap(SharePost::getId, post -> post));
        
        for (UserBehavior behavior : behaviors) {
            Long postId = behavior.getTargetId();
            SharePost post = postMap.get(postId);
            if (post != null && post.getLocation() != null && !post.getLocation().isEmpty()) {
                String location = post.getLocation().trim();
                double behaviorWeight = postBehaviorWeights.getOrDefault(postId, 1.0);
                locationWeights.put(location, locationWeights.getOrDefault(location, 0.0) + behaviorWeight);
            }
        }

        // 归一化标签权重
        double totalTagWeight = tagWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalTagWeight > 0) {
            tagWeights.replaceAll((k, v) -> v / totalTagWeight);
        }

        // 归一化地点权重
        double totalLocationWeight = locationWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalLocationWeight > 0) {
            locationWeights.replaceAll((k, v) -> v / totalLocationWeight);
        }

        return new UserProfile(tagWeights, locationWeights);
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
     * 用户偏好画像内部类
     */
    private static class UserProfile {
        final Map<String, Double> tagWeights;
        final Map<String, Double> locationWeights;

        UserProfile() {
            this.tagWeights = Collections.emptyMap();
            this.locationWeights = Collections.emptyMap();
        }

        UserProfile(Map<String, Double> tagWeights, Map<String, Double> locationWeights) {
            this.tagWeights = tagWeights;
            this.locationWeights = locationWeights;
        }

        boolean isEmpty() {
            return tagWeights.isEmpty() && locationWeights.isEmpty();
        }
    }
}

