package com.example.demo1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 推荐结果DTO
 * 用于封装推荐算法返回的结果
 */
@Data
@AllArgsConstructor
public class RecommendationResult {
    /**
     * 动态ID
     */
    private Long postId;
    
    /**
     * 推荐分数
     */
    private Double score;
    
    /**
     * 推荐原因
     */
    private String reason;
}

