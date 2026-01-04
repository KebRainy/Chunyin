package com.example.demo1.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG系统配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RAGConfig {
    /**
     * 阿里云API配置
     */
    private Aliyun aliyun = new Aliyun();
    
    /**
     * Milvus配置
     */
    private Milvus milvus = new Milvus();
    
    /**
     * 向量维度
     */
    private Integer vectorDimension = 1536;
    
    /**
     * 检索Top-K数量
     */
    private Integer topK = 10;
    
    /**
     * LLM温度参数
     */
    private Double temperature = 0.7;
    
    /**
     * 用户行为权重配置
     */
    private BehaviorWeight behaviorWeight = new BehaviorWeight();
    
    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String region = "cn-hangzhou";
        private String llmModel = "qwen-plus";
        private String embeddingModel = "text-embedding-v2";
    }
    
    @Data
    public static class Milvus {
        private String host = "localhost";
        private Integer port = 19530;
        private String collectionName = "beverage_vectors";
    }
    
    @Data
    public static class BehaviorWeight {
        private Double view = 1.0;
        private Double like = 3.0;
        private Double favorite = 5.0;
        private Double comment = 4.0;
        private Double share = 2.0;
    }
}


