package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Wiki内容VO
 */
@Data
@Builder
public class WikiContentVO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String slug;
    private Double similarity; // 相似度分数
}

