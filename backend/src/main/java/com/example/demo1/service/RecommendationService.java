package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.entity.PostTag;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.mapper.PostTagMapper;
import com.example.demo1.mapper.SharePostMapper;
import com.example.demo1.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务
 * 基于协同过滤算法实现内容推荐
 * 实现混合推荐策略：基于内容推荐(40%) + 协同过滤推荐(40%) + 热度排行推荐(20%)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final PostTagMapper postTagMapper;
    private final SharePostMapper sharePostMapper;
    private final SharePostService sharePostService;

    // 推荐策略权重
    private static final double CONTENT_BASED_WEIGHT = 0.4;
    private static final double COLLABORATIVE_FILTERING_WEIGHT = 0.4;
    private static final double HOT_RANKING_WEIGHT = 0.2;

    // 协同过滤参数
    private static final int SIMILAR_USER_LIMIT = 10; // Top-K相似用户数
    private static final int CANDIDATE_POST_LIMIT = 1000; // 候选动态数量

    /**
     * 获取推荐动态
     * 混合推荐策略：基于内容推荐(40%) + 协同过滤推荐(40%) + 热度排行推荐(20%)
     */
    public List<SharePostVO> getRecommendedPosts(Long userId, int size) {
        if (userId == null) {
            // 未登录用户返回热门动态
            return sharePostService.getHotPosts(
                com.example.demo1.common.enums.TimeDimension.WEEK, 1, size, null
            ).getItems();
        }

        // 1. 获取候选动态集合
        List<SharePost> candidatePosts = getCandidatePosts(userId, CANDIDATE_POST_LIMIT);
        if (candidatePosts.isEmpty()) {
            return sharePostService.getHotPosts(
                com.example.demo1.common.enums.TimeDimension.WEEK, 1, size, userId
            ).getItems();
        }

        // 2. 并行调用三种推荐算法
        List<RecommendationResult> contentBasedResults = contentBasedRecommend(userId, candidatePosts, size * 2);
        List<RecommendationResult> collaborativeResults = collaborativeFilteringRecommend(userId, candidatePosts, size * 2);
        List<RecommendationResult> hotResults = hotRankingRecommend(candidatePosts, size * 2);

        // 3. 合并推荐结果
        List<RecommendationResult> mergedResults = mergeRecommendations(
            contentBasedResults, collaborativeResults, hotResults
        );

        // 4. 去重并按分数排序，取Top-N
        List<RecommendationResult> finalResults = mergedResults.stream()
            .collect(Collectors.toMap(
                r -> r.postId,
                r -> r,
                (r1, r2) -> r1.score > r2.score ? r1 : r2
            ))
            .values()
            .stream()
            .sorted(Comparator.comparingDouble((RecommendationResult r) -> r.score).reversed())
            .limit(size)
            .collect(Collectors.toList());

        if (finalResults.isEmpty()) {
            return sharePostService.getHotPosts(
                com.example.demo1.common.enums.TimeDimension.WEEK, 1, size, userId
            ).getItems();
        }

        // 5. 转换为VO
        return finalResults.stream()
            .map(result -> {
                try {
                    return sharePostService.getPostById(result.postId, userId, false);
                } catch (Exception e) {
                    log.warn("获取推荐动态失败: postId={}", result.postId, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 基于内容的推荐
     * 基于用户标签偏好和动态标签匹配度
     */
    private List<RecommendationResult> contentBasedRecommend(Long userId, List<SharePost> candidatePosts, int limit) {
        // 1. 构建用户偏好画像
        Map<String, Double> userTagPreference = buildUserTagPreference(userId);
        
        if (userTagPreference.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 计算每个动态的标签匹配分数
        List<RecommendationResult> results = candidatePosts.stream()
            .map(post -> {
                double score = calculateContentBasedScore(post, userTagPreference);
                return new RecommendationResult(post.getId(), score, "标签匹配");
            })
            .filter(r -> r.score > 0)
            .sorted(Comparator.comparingDouble((RecommendationResult r) -> r.score).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return results;
    }

    /**
     * 协同过滤推荐
     * 基于相似用户的行为推荐
     */
    private List<RecommendationResult> collaborativeFilteringRecommend(Long userId, List<SharePost> candidatePosts, int limit) {
        // 1. 找到相似用户
        List<Long> similarUserIds = findSimilarUsers(userId, SIMILAR_USER_LIMIT);
        
        if (similarUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取相似用户喜欢的动态
        Set<Long> similarUserLikedPostIds = getSimilarUserLikedPosts(similarUserIds, userId);
        
        if (similarUserLikedPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 计算候选动态的协同过滤分数
        Map<Long, Double> userSimilarityMap = calculateUserSimilarities(userId, similarUserIds);
        
        List<RecommendationResult> results = candidatePosts.stream()
            .filter(post -> similarUserLikedPostIds.contains(post.getId()))
            .map(post -> {
                double score = calculateCollaborativeScore(post, similarUserIds, userSimilarityMap);
                return new RecommendationResult(post.getId(), score, "相似用户喜欢");
            })
            .filter(r -> r.score > 0)
            .sorted(Comparator.comparingDouble((RecommendationResult r) -> r.score).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return results;
    }

    /**
     * 热度排行推荐
     * 基于动态的热度分数
     */
    private List<RecommendationResult> hotRankingRecommend(List<SharePost> candidatePosts, int limit) {
        LocalDateTime now = LocalDateTime.now();
        
        List<RecommendationResult> results = candidatePosts.stream()
            .map(post -> {
                double score = calculateHotScore(post, now);
                return new RecommendationResult(post.getId(), score, "热门内容");
            })
            .sorted(Comparator.comparingDouble((RecommendationResult r) -> r.score).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return results;
    }

    /**
     * 获取用户互动向量
     * 浏览: 权重1.0, 点赞: 权重2.0, 收藏: 权重3.0, 评论: 权重1.5
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
                return 1.0;
            case LIKE:
                return 2.0;
            case FAVORITE:
                return 3.0;
            case COMMENT:
                return 1.5;
            case SHARE:
                return 2.0;
            default:
                return 1.0;
        }
    }

    /**
     * 计算用户相似度（余弦相似度）
     */
    private double calculateUserSimilarity(Long user1Id, Long user2Id) {
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

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // 计算点积和范数
        for (Long postId : commonPostIds) {
            double v1 = vector1.get(postId);
            double v2 = vector2.get(postId);
            dotProduct += v1 * v2;
        }

        for (Double value : vector1.values()) {
            norm1 += value * value;
        }

        for (Double value : vector2.values()) {
            norm2 += value * value;
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * 查找相似用户（Top-K）
     */
    private List<Long> findSimilarUsers(Long userId, int limit) {
        // 获取所有有行为的用户ID
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
     * 构建用户标签偏好
     */
    private Map<String, Double> buildUserTagPreference(Long userId) {
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .ge(UserBehavior::getCreatedAt, LocalDateTime.now().minusDays(30))
        );

        if (behaviors.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> postIds = behaviors.stream()
            .map(UserBehavior::getTargetId)
            .collect(Collectors.toSet());

        List<PostTag> tags = postTagMapper.selectList(
            new LambdaQueryWrapper<PostTag>()
                .in(PostTag::getPostId, postIds)
        );

        Map<String, Double> tagScores = new HashMap<>();
        Map<Long, Double> postBehaviorWeight = behaviors.stream()
            .collect(Collectors.toMap(
                UserBehavior::getTargetId,
                b -> getBehaviorWeight(b.getBehaviorType()),
                (a, b) -> a + b
            ));

        for (PostTag tag : tags) {
            Long postId = tag.getPostId();
            double weight = postBehaviorWeight.getOrDefault(postId, 1.0);
            String tagName = tag.getTagName();
            tagScores.put(tagName, tagScores.getOrDefault(tagName, 0.0) + weight);
        }

        // 归一化
        double totalScore = tagScores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore > 0) {
            tagScores.replaceAll((k, v) -> v / totalScore);
        }

        return tagScores;
    }

    /**
     * 计算基于内容的推荐分数（标签匹配度）
     */
    private double calculateContentBasedScore(SharePost post, Map<String, Double> userTagPreference) {
        List<PostTag> postTags = postTagMapper.selectList(
            new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, post.getId())
        );

        if (postTags.isEmpty()) {
            return 0.0;
        }

        // 计算标签匹配度（余弦相似度）
        double dotProduct = 0.0;
        double userNorm = 0.0;
        double postNorm = 0.0;

        Set<String> postTagNames = postTags.stream()
            .map(PostTag::getTagName)
            .collect(Collectors.toSet());

        // 计算用户向量范数
        for (Double value : userTagPreference.values()) {
            userNorm += value * value;
        }
        userNorm = Math.sqrt(userNorm);

        // 计算动态向量（每个标签权重相等）
        double postTagWeight = 1.0 / postTagNames.size();

        // 计算点积和动态向量范数
        for (String tagName : postTagNames) {
            double userWeight = userTagPreference.getOrDefault(tagName, 0.0);
            dotProduct += userWeight * postTagWeight;
            postNorm += postTagWeight * postTagWeight;
        }
        postNorm = Math.sqrt(postNorm);

        if (userNorm == 0.0 || postNorm == 0.0) {
            return 0.0;
        }

        return dotProduct / (userNorm * postNorm);
    }

    /**
     * 计算热度分数
     * 公式: 基础分 = viewCount×0.1 + likeCount×2 + favoriteCount×3 + commentCount×1.5
     * 时间衰减 = 1 / (1 + hoursSinceCreated/24)
     * 热度分 = 基础分 × 时间衰减
     */
    private double calculateHotScore(SharePost post, LocalDateTime now) {
        int viewCount = post.getViewCount() != null ? post.getViewCount() : 0;
        int likeCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
        int favoriteCount = post.getFavoriteCount() != null ? post.getFavoriteCount() : 0;
        int commentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;

        // 基础分
        double baseScore = viewCount * 0.1 + likeCount * 2.0 + 
                          favoriteCount * 3.0 + commentCount * 1.5;

        // 时间衰减
        LocalDateTime createdAt = post.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        long hoursSinceCreated = Duration.between(createdAt, now).toHours();
        double timeDecay = 1.0 / (1.0 + hoursSinceCreated / 24.0);

        // 热度分
        return baseScore * timeDecay;
    }

    /**
     * 获取候选动态集合
     */
    private List<SharePost> getCandidatePosts(Long userId, int limit) {
        // 获取用户已看过的动态
        Set<Long> viewedPostIds = userBehaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getTargetType, TargetType.POST)
                .select(UserBehavior::getTargetId)
        ).stream()
        .map(UserBehavior::getTargetId)
        .collect(Collectors.toSet());

        // 获取候选动态（排除已看过的，最近90天）
        return sharePostMapper.selectList(
            new LambdaQueryWrapper<SharePost>()
                .notIn(!viewedPostIds.isEmpty(), SharePost::getId, viewedPostIds)
                .ge(SharePost::getCreatedAt, LocalDateTime.now().minusDays(90))
                .orderByDesc(SharePost::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    /**
     * 合并推荐结果
     * 按策略权重调整分数
     */
    private List<RecommendationResult> mergeRecommendations(
            List<RecommendationResult> contentBased,
            List<RecommendationResult> collaborative,
            List<RecommendationResult> hot) {
        
        List<RecommendationResult> merged = new ArrayList<>();

        // 调整分数并添加
        for (RecommendationResult result : contentBased) {
            merged.add(new RecommendationResult(
                result.postId, 
                result.score * CONTENT_BASED_WEIGHT, 
                result.reason
            ));
        }

        for (RecommendationResult result : collaborative) {
            merged.add(new RecommendationResult(
                result.postId, 
                result.score * COLLABORATIVE_FILTERING_WEIGHT, 
                result.reason
            ));
        }

        for (RecommendationResult result : hot) {
            merged.add(new RecommendationResult(
                result.postId, 
                result.score * HOT_RANKING_WEIGHT, 
                result.reason
            ));
        }

        return merged;
    }

    /**
     * 获取相似动态
     * 基于标签相似度
     */
    public List<SharePostVO> getSimilarPosts(Long postId, Long userId, int size) {
        // 1. 获取原动态的标签
        List<PostTag> sourceTags = postTagMapper.selectList(
            new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, postId)
        );

        if (sourceTags.isEmpty()) {
            return sharePostService.listPosts(1, size, userId).getItems();
        }

        Set<String> sourceTagNames = sourceTags.stream()
            .map(PostTag::getTagName)
            .collect(Collectors.toSet());

        // 2. 查找包含相同标签的动态
        List<PostTag> relatedTags = postTagMapper.selectList(
            new LambdaQueryWrapper<PostTag>()
                .in(PostTag::getTagName, sourceTagNames)
                .ne(PostTag::getPostId, postId)
        );

        if (relatedTags.isEmpty()) {
            return sharePostService.listPosts(1, size, userId).getItems();
        }

        // 3. 计算每个动态的相似度分数
        Map<Long, Integer> postTagCounts = relatedTags.stream()
            .collect(Collectors.groupingBy(
                PostTag::getPostId,
                Collectors.collectingAndThen(Collectors.counting(), count -> count.intValue())
            ));

        // 4. 按相似度排序
        List<Map.Entry<Long, Integer>> sortedPosts = postTagCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(size)
            .collect(Collectors.toList());

        // 5. 转换为VO
        return sortedPosts.stream()
            .map(entry -> {
                try {
                    return sharePostService.getPostById(entry.getKey(), userId, false);
                } catch (Exception e) {
                    log.warn("获取相似动态失败: postId={}", entry.getKey(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 推荐结果内部类
     */
    private static class RecommendationResult {
        final Long postId;
        final double score;
        final String reason;

        RecommendationResult(Long postId, double score, String reason) {
            this.postId = postId;
            this.score = score;
            this.reason = reason;
        }
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
