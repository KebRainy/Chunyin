package com.example.demo1.algorithm;

import com.example.demo1.entity.SharePost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 热度分数计算器
 * 计算内容的热度分数，综合多个互动指标和时间衰减
 */
@Component
@Slf4j
public class HotScoreCalculator {

    // 互动指标权重配置
    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 2.0;
    private static final double FAVORITE_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 1.5;

    /**
     * 计算动态的热度分数
     * 
     * 公式:
     *   基础分 = viewCount×0.1 + likeCount×2 + favoriteCount×3 + commentCount×1.5
     *   时间衰减 = 1 / (1 + hoursSinceCreated/24)
     *   热度分 = 基础分 × 时间衰减
     * 
     * @param post 动态对象
     * @param now 当前时间
     * @return 热度分数
     */
    public double calculateHotScore(SharePost post, LocalDateTime now) {
        int viewCount = post.getViewCount() != null ? post.getViewCount() : 0;
        int likeCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
        int favoriteCount = post.getFavoriteCount() != null ? post.getFavoriteCount() : 0;
        int commentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;

        // 计算基础分
        double baseScore = viewCount * VIEW_WEIGHT + 
                          likeCount * LIKE_WEIGHT + 
                          favoriteCount * FAVORITE_WEIGHT + 
                          commentCount * COMMENT_WEIGHT;

        // 计算时间衰减
        double timeDecay = calculateTimeDecay(post.getCreatedAt(), now);

        // 热度分 = 基础分 × 时间衰减
        return baseScore * timeDecay;
    }

    /**
     * 计算时间衰减系数
     * 
     * 公式: 时间衰减 = 1 / (1 + hoursSinceCreated/24)
     * 
     * 逻辑:
     *   1. 计算发布时间到现在的小时数
     *   2. 应用衰减公式
     *   3. 越新的内容衰减越小（分数越高）
     * 
     * @param createdAt 创建时间
     * @param now 当前时间
     * @return 时间衰减系数（0-1之间）
     */
    public double calculateTimeDecay(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null) {
            createdAt = now;
        }

        // 计算发布时间到现在的小时数
        long hoursSinceCreated = Duration.between(createdAt, now).toHours();
        
        // 如果时间差为负数（未来时间），返回1.0
        if (hoursSinceCreated < 0) {
            return 1.0;
        }

        // 应用衰减公式: 1 / (1 + hoursSinceCreated/24)
        // 例如：
        //   0小时: 1 / (1 + 0/24) = 1.0
        //   24小时: 1 / (1 + 24/24) = 0.5
        //   48小时: 1 / (1 + 48/24) = 0.33
        //   72小时: 1 / (1 + 72/24) = 0.25
        double timeDecay = 1.0 / (1.0 + hoursSinceCreated / 24.0);

        // 确保在0-1之间
        return Math.max(0.0, Math.min(1.0, timeDecay));
    }

    /**
     * 批量计算热度分数并排序
     * 
     * @param posts 动态列表
     * @param now 当前时间
     * @param limit 返回数量限制
     * @return 按热度分数排序的动态ID列表
     */
    public List<Long> rankByHotScore(List<SharePost> posts, LocalDateTime now, int limit) {
        return posts.stream()
            .map(post -> {
                double score = calculateHotScore(post, now);
                return new PostScore(post.getId(), score);
            })
            .sorted(Comparator.comparingDouble((PostScore ps) -> ps.score).reversed())
            .limit(limit)
            .map(ps -> ps.postId)
            .collect(Collectors.toList());
    }

    /**
     * 动态分数内部类
     */
    private static class PostScore {
        final Long postId;
        final double score;

        PostScore(Long postId, double score) {
            this.postId = postId;
            this.score = score;
        }
    }
}

