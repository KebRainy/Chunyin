package com.example.demo1.rag.client;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.example.demo1.config.RAGConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云文本嵌入客户端
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AliyunEmbeddingClient {
    
    private final RAGConfig ragConfig;
    private final TextEmbedding textEmbedding = new TextEmbedding();
    
    /**
     * 获取文本的向量表示
     */
    public List<Float> getEmbedding(String text) {
        try {
            List<String> texts = new ArrayList<>();
            texts.add(text);
            
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(ragConfig.getAliyun().getAccessKeySecret())
                    .model(ragConfig.getAliyun().getEmbeddingModel())
                    .texts(texts)
                    .build();
            
            com.alibaba.dashscope.embeddings.TextEmbeddingResult result = textEmbedding.call(param);
            
            if (result.getOutput() != null && result.getOutput().getEmbeddings() != null 
                    && !result.getOutput().getEmbeddings().isEmpty()) {
                // 转换Double到Float
                List<Double> doubleList = result.getOutput().getEmbeddings().get(0).getEmbedding();
                List<Float> floatList = new ArrayList<>();
                for (Double d : doubleList) {
                    floatList.add(d.floatValue());
                }
                return floatList;
            }
            
            log.warn("获取向量失败，返回空向量");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用文本嵌入API失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 批量获取文本向量
     */
    public List<List<Float>> getEmbeddings(List<String> texts) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(ragConfig.getAliyun().getAccessKeySecret())
                    .model(ragConfig.getAliyun().getEmbeddingModel())
                    .texts(texts)
                    .build();
            
            com.alibaba.dashscope.embeddings.TextEmbeddingResult result = textEmbedding.call(param);
            
            List<List<Float>> embeddings = new ArrayList<>();
            if (result.getOutput() != null && result.getOutput().getEmbeddings() != null) {
                for (var embedding : result.getOutput().getEmbeddings()) {
                    // 转换Double到Float
                    List<Double> doubleList = embedding.getEmbedding();
                    List<Float> floatList = new ArrayList<>();
                    for (Double d : doubleList) {
                        floatList.add(d.floatValue());
                    }
                    embeddings.add(floatList);
                }
            }
            
            return embeddings;
        } catch (Exception e) {
            log.error("批量获取向量失败", e);
            return new ArrayList<>();
        }
    }
}
