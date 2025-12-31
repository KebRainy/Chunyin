package com.example.demo1.common.enums;

import lombok.Getter;

/**
 * 酒吧排序策略枚举
 * SF-11: 附近最佳酒吧排序
 */
@Getter
public enum BarSortStrategy {
    /**
     * 综合排序（默认）
     * 距离和评分各占50%权重
     */
    COMPREHENSIVE(0.5, 0.5),
    
    /**
     * 距离优先
     * 距离占70%，评分占30%
     */
    DISTANCE_FIRST(0.7, 0.3),
    
    /**
     * 评分优先
     * 评分占70%，距离占30%
     */
    RATING_FIRST(0.3, 0.7),
    
    /**
     * 仅按距离排序
     */
    DISTANCE_ONLY(1.0, 0.0),
    
    /**
     * 仅按评分排序
     */
    RATING_ONLY(0.0, 1.0);
    
    /**
     * 距离权重
     */
    private final double distanceWeight;
    
    /**
     * 评分权重
     */
    private final double ratingWeight;
    
    BarSortStrategy(double distanceWeight, double ratingWeight) {
        this.distanceWeight = distanceWeight;
        this.ratingWeight = ratingWeight;
    }
}

