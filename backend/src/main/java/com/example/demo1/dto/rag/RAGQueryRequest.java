package com.example.demo1.dto.rag;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG查询请求DTO
 */
@Data
public class RAGQueryRequest {
    /**
     * 用户自然语言查询
     */
    private String query;
    
    /**
     * 返回结果数量
     */
    private Integer topK = 10;
    
    /**
     * 是否包含推荐理由
     */
    private Boolean includeReason = true;
    
    /**
     * 对话历史（用于多轮交互）
     */
    private List<ConversationTurn> conversationHistory;
    
    /**
     * 对话轮次
     */
    @Data
    public static class ConversationTurn {
        /**
         * 用户问题
         */
        private String userQuery;
        
        /**
         * AI回答
         */
        private String aiResponse;
    }
}

