package com.example.demo1.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档分块策略
 * 将长文档分割成适合向量化的块
 */
@Component
@Slf4j
public class ChunkingStrategy {
    
    /**
     * 默认块大小（字符数）
     */
    private static final int DEFAULT_CHUNK_SIZE = 500;
    
    /**
     * 块重叠大小（字符数）
     */
    private static final int DEFAULT_CHUNK_OVERLAP = 50;
    
    /**
     * 按大小分块
     */
    public List<String> chunkBySize(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        
        if (textLength <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        
        int start = 0;
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            // 尝试在句号、换行符等位置截断
            if (end < textLength) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int lastBreak = Math.max(lastPeriod, lastNewline);
                
                if (lastBreak > start + chunkSize / 2) {
                    end = lastBreak + 1;
                }
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            // 移动到下一个块的起始位置（考虑重叠）
            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }
        
        return chunks;
    }
    
    /**
     * 按段落分块（适用于Markdown）
     */
    public List<String> chunkByParagraph(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            
            // 如果当前块加上新段落超过大小限制，保存当前块
            if (currentChunk.length() + paragraph.length() > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * 智能分块（结合大小和段落）
     */
    public List<String> smartChunk(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 如果是Markdown格式，优先按段落分块
        if (isMarkdown(text)) {
            List<String> paragraphChunks = chunkByParagraph(text);
            List<String> finalChunks = new ArrayList<>();
            
            // 如果段落块太大，再按大小分割
            for (String chunk : paragraphChunks) {
                if (chunk.length() > DEFAULT_CHUNK_SIZE * 2) {
                    finalChunks.addAll(chunkBySize(chunk, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP));
                } else {
                    finalChunks.add(chunk);
                }
            }
            
            return finalChunks;
        }
        
        // 普通文本按大小分块
        return chunkBySize(text, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }
    
    /**
     * 判断是否为Markdown格式
     */
    private boolean isMarkdown(String text) {
        // 简单的Markdown特征检测
        Pattern markdownPattern = Pattern.compile(
                "^(#{1,6}\\s+|\\*\\*|\\*|`|```|\\[|!\\[|>|\\-|\\+|\\d+\\.)",
                Pattern.MULTILINE
        );
        return markdownPattern.matcher(text).find();
    }
}



