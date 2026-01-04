package com.example.demo1.dto.rag;

import com.example.demo1.dto.response.BeverageSummaryVO;
import com.example.demo1.dto.response.WikiContentVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG推荐响应DTO
 */
@Data
@Builder
public class RAGRecommendationResponse {
    /**
     * 推荐的酒类列表（已废弃，保留以兼容旧接口）
     */
    @Deprecated
    private List<BeverageSummaryVO> beverages;
    
    /**
     * 推荐的Wiki内容列表
     */
    private List<WikiContentVO> wikiContents;
    
    /**
     * LLM生成的推荐理由
     */
    private String recommendationReason;
    
    /**
     * 查询理解结果（关键词、意图等）
     */
    private QueryUnderstanding queryUnderstanding;
    
    @Data
    @Builder
    public static class QueryUnderstanding {
        private List<String> keywords;
        private String intent;
        private String scene; // 场景：晚餐、聚会等
        private String tastePreference; // 口味偏好
    }
}

