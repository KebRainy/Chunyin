package com.example.demo1.dto.rag;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户偏好VO
 */
@Data
@Builder
public class UserPreferenceVO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 类型偏好（类型 -> 权重）
     */
    private Map<String, Double> typePreference;
    
    /**
     * 口味偏好（关键词 -> 权重）
     */
    private Map<String, Double> tastePreference;
    
    /**
     * 产地偏好（产地 -> 权重）
     */
    private Map<String, Double> originPreference;
    
    /**
     * 价格偏好区间
     */
    private PriceRange priceRange;
    
    /**
     * 偏好向量（用于向量检索）
     */
    private List<Float> preferenceVector;
    
    @Data
    @Builder
    public static class PriceRange {
        private Double minPrice;
        private Double maxPrice;
    }
}


