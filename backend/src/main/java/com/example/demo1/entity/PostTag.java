package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo1.common.enums.TagCategory;
import com.example.demo1.common.enums.TagSource;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态标签实体
 */
@Data
@TableName("post_tag")
public class PostTag {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 动态ID
     */
    private Long postId;
    
    /**
     * 标签名称
     */
    private String tagName;
    
    /**
     * 标签分类
     * 注意：数据库中只支持 BEVERAGE_TYPE, TASTE, SCENE, LOCATION, OTHER
     */
    private TagCategory tagCategory;
    
    /**
     * 标签来源（用户输入或自动提取）
     */
    private TagSource source;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

