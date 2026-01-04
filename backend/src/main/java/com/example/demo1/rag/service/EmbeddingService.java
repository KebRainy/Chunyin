package com.example.demo1.rag.service;

import com.example.demo1.rag.client.AliyunEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本嵌入服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    
    private final AliyunEmbeddingClient embeddingClient;
    
    /**
     * 将文本转换为向量
     */
    public List<Float> embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return embeddingClient.getEmbedding(text);
    }
    
    /**
     * 批量将文本转换为向量
     */
    public List<List<Float>> embedTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        return embeddingClient.getEmbeddings(texts);
    }
    
    /**
     * 将向量转换为JSON字符串
     */
    public String vectorToJson(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        return "[" + vector.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("") + "]";
    }
    
    /**
     * 从JSON字符串解析向量
     */
    public List<Float> jsonToVector(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        
        List<Float> vector = new ArrayList<>();
        try {
            String cleaned = json.trim().replace("[", "").replace("]", "");
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty()) {
                    vector.add(Float.parseFloat(part));
                }
            }
        } catch (Exception e) {
            log.error("解析向量JSON失败: {}", json, e);
        }
        return vector;
    }
}


