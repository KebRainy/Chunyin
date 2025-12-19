package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BarReviewVO {
    private Long id;
    private Long barId;
    private Long userId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    
    // 用户信息
    private SimpleUserVO user;
}

