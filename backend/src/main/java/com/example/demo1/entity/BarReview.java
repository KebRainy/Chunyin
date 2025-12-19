package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bar_review")
public class BarReview {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long barId;
    private Long userId;
    
    // 评分 (1-5分)
    private Integer rating;
    
    // 评价内容
    private String content;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean isActive = true;
}

