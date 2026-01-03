package com.example.demo1.algorithm;

import com.example.demo1.dto.response.BarVO;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 酒吧推荐排序算法
 * 负责对酒吧列表进行排序，平衡距离和质量两个因素
 */
public class BarRankingAlgorithm {

    /**
     * 默认距离权重
     */
    private static final double DEFAULT_DISTANCE_WEIGHT = 0.6;
    
    /**
     * 默认质量权重
     */
    private static final double DEFAULT_QUALITY_WEIGHT = 0.4;

    /**
     * 对酒吧列表进行排序
     * 
     * @param bars 酒吧列表
     * @param userLocation 用户位置（纬度，经度）
     * @param preferences 用户偏好（距离权重，质量权重），如果为null则使用默认权重
     * @return 排序后的酒吧推荐结果列表
     */
    public static List<BarRecommendationResult> rankBars(
            List<BarVO> bars,
            UserLocation userLocation,
            RankingPreferences preferences) {
        
        if (bars == null || bars.isEmpty()) {
            return List.of();
        }
        
        if (userLocation == null) {
            throw new IllegalArgumentException("用户位置不能为空");
        }
        
        // 使用默认权重或用户偏好权重
        double distanceWeight = preferences != null ? preferences.getDistanceWeight() : DEFAULT_DISTANCE_WEIGHT;
        double qualityWeight = preferences != null ? preferences.getQualityWeight() : DEFAULT_QUALITY_WEIGHT;
        
        // 计算每个酒吧的推荐分数
        List<BarRecommendationResult> results = bars.stream()
                .map(bar -> {
                    // 计算距离
                    double distance = calculateDistance(
                            userLocation.getLatitude(),
                            userLocation.getLongitude(),
                            bar.getLatitude(),
                            bar.getLongitude()
                    );
                    
                    // 计算质量分数
                    double qualityScore = calculateQualityScore(bar);
                    
                    // 归一化距离（假设最大距离为50公里）
                    double maxDistance = 50.0;
                    double normalizedDistance = Math.min(distance / maxDistance, 1.0);
                    
                    // 计算综合分数
                    double compositeScore = calculateCompositeScore(
                            normalizedDistance,
                            qualityScore,
                            distanceWeight,
                            qualityWeight
                    );
                    
                    return new BarRecommendationResult(bar, distance, qualityScore, compositeScore);
                })
                .sorted((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()))
                .collect(Collectors.toList());
        
        return results;
    }

    /**
     * 计算两点之间的距离（Haversine公式）
     * 
     * @param lat1 点1纬度
     * @param lon1 点1经度
     * @param lat2 点2纬度
     * @param lon2 点2经度
     * @return 距离（公里）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 计算酒吧质量分数
     * 公式: 质量分 = avgRating / 5.0 × 0.7 + min(reviewCount, 100) / 100 × 0.3
     * 
     * @param bar 酒吧信息
     * @return 质量分数（0-1之间）
     */
    public static double calculateQualityScore(BarVO bar) {
        // 评分部分（0-1之间）
        double rating = bar.getAvgRating() != null ? bar.getAvgRating() : 0.0;
        double ratingScore = (rating / 5.0) * 0.7;
        
        // 评价数量部分（0-1之间，最多100条评价）
        int reviewCount = bar.getReviewCount() != null ? bar.getReviewCount() : 0;
        double reviewCountScore = (Math.min(reviewCount, 100) / 100.0) * 0.3;
        
        // 综合质量分数
        double qualityScore = ratingScore + reviewCountScore;
        
        // 确保在0-1之间
        return Math.max(0.0, Math.min(1.0, qualityScore));
    }

    /**
     * 计算综合分数
     * 公式: 综合分 = distanceWeight × (1 - 归一化距离) + qualityWeight × 质量分
     * 
     * @param normalizedDistance 归一化后的距离（0-1之间）
     * @param qualityScore 质量分数（0-1之间）
     * @param distanceWeight 距离权重
     * @param qualityWeight 质量权重
     * @return 综合分数（0-1之间）
     */
    public static double calculateCompositeScore(
            double normalizedDistance,
            double qualityScore,
            double distanceWeight,
            double qualityWeight) {
        
        // 距离分数：距离越近分数越高
        double distanceScore = 1.0 - normalizedDistance;
        
        // 综合分数
        double compositeScore = distanceWeight * distanceScore + qualityWeight * qualityScore;
        
        // 确保在0-1之间
        return Math.max(0.0, Math.min(1.0, compositeScore));
    }

    /**
     * 用户位置
     */
    @Data
    public static class UserLocation {
        private final double latitude;
        private final double longitude;
    }

    /**
     * 排序偏好
     */
    @Data
    public static class RankingPreferences {
        private final double distanceWeight;
        private final double qualityWeight;
        
        public RankingPreferences(double distanceWeight, double qualityWeight) {
            if (distanceWeight < 0 || distanceWeight > 1 || qualityWeight < 0 || qualityWeight > 1) {
                throw new IllegalArgumentException("权重必须在0-1之间");
            }
            if (Math.abs(distanceWeight + qualityWeight - 1.0) > 0.01) {
                throw new IllegalArgumentException("距离权重和质量权重之和必须等于1");
            }
            this.distanceWeight = distanceWeight;
            this.qualityWeight = qualityWeight;
        }
    }

    /**
     * 酒吧推荐结果
     */
    @Data
    public static class BarRecommendationResult {
        private final BarVO bar;
        private final double distance;
        private final double qualityScore;
        private final double compositeScore;
    }
}

