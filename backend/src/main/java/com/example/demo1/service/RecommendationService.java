package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.algorithm.ContentBasedRecommender;
import com.example.demo1.algorithm.CollaborativeFilteringRecommender;
import com.example.demo1.algorithm.HotScoreCalculator;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.dto.response.RecommendationResult;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务
 * 实现混合推荐策略：基于内容推荐(40%) + 协同过滤推荐(40%) + 热度排行推荐(20%)
 * 
 * 推荐流程说明：
 * 1. 获取候选动态集合（排除用户已看过的，最近90天）
 * 2. 并行调用三种推荐算法：
 *    - ContentBasedRecommender: 基于用户标签和地点偏好匹配
 *    - CollaborativeFilteringRecommender: 基于相似用户的行为推荐
 *    - HotScoreCalculator: 基于热度分数（互动指标+时间衰减）
 * 3. 按权重合并推荐结果
 * 4. 去重并按分数排序，取Top-N
 * 5. 转换为VO返回
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final PostTagMapper postTagMapper;
    private final SharePostMapper sharePostMapper;
    private final SharePostService sharePostService;
    
    // 三个推荐器
    private final ContentBasedRecommender contentBasedRecommender;
    private final CollaborativeFilteringRecommender collaborativeFilteringRecommender;
    private final HotScoreCalculator hotScoreCalculator;

    // 推荐策略权重
    private static final double CONTENT_BASED_WEIGHT = 0.4;
    private static final double COLLABORATIVE_FILTERING_WEIGHT = 0.4;
    private static final double HOT_RANKING_WEIGHT = 0.2;

    // 候选动态数量
    private static final int CANDIDATE_POST_LIMIT = 1000;

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

        // 2. 调用三种推荐算法
        List<RecommendationResult> contentBasedResults = contentBasedRecommender.recommend(userId, candidatePosts, size * 2);
        List<RecommendationResult> collaborativeResults = collaborativeFilteringRecommender.recommend(userId, candidatePosts, size * 2);
        List<RecommendationResult> hotResults = hotRankingRecommend(candidatePosts, size * 2);

        // 3. 合并推荐结果
        List<RecommendationResult> mergedResults = mergeRecommendations(
            contentBasedResults, collaborativeResults, hotResults
        );

        // 4. 去重并按分数排序，取Top-N
        List<RecommendationResult> finalResults = mergedResults.stream()
            .collect(Collectors.toMap(
                RecommendationResult::getPostId,
                r -> r,
                (r1, r2) -> r1.getScore() > r2.getScore() ? r1 : r2
            ))
            .values()
            .stream()
            .sorted(Comparator.comparingDouble(RecommendationResult::getScore).reversed())
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
                    return sharePostService.getPostById(result.getPostId(), userId, false);
                } catch (Exception e) {
                    log.warn("获取推荐动态失败: postId={}", result.getPostId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 热度排行推荐
     * 基于动态的热度分数
     */
    private List<RecommendationResult> hotRankingRecommend(List<SharePost> candidatePosts, int limit) {
        LocalDateTime now = LocalDateTime.now();
        
        List<RecommendationResult> results = candidatePosts.stream()
            .map(post -> {
                double score = hotScoreCalculator.calculateHotScore(post, now);
                return new RecommendationResult(post.getId(), score, "热门内容");
            })
            .sorted(Comparator.comparingDouble(RecommendationResult::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return results;
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
                result.getPostId(), 
                result.getScore() * CONTENT_BASED_WEIGHT, 
                result.getReason()
            ));
        }

        for (RecommendationResult result : collaborative) {
            merged.add(new RecommendationResult(
                result.getPostId(), 
                result.getScore() * COLLABORATIVE_FILTERING_WEIGHT, 
                result.getReason()
            ));
        }

        for (RecommendationResult result : hot) {
            merged.add(new RecommendationResult(
                result.getPostId(), 
                result.getScore() * HOT_RANKING_WEIGHT, 
                result.getReason()
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

}
