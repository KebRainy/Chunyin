package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.TargetType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为记录实体
 * 用于推荐系统的行为分析
 */
@Data
@TableName("user_behavior")
public class UserBehavior {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 目标类型（POST, BEVERAGE, WIKI, BAR）
     */
    private TargetType targetType;
    
    /**
     * 目标ID
     */
    private Long targetId;
    
    /**
     * 行为类型（VIEW, LIKE, FAVORITE, COMMENT, SHARE）
     */
    private BehaviorType behaviorType;
    
    /**
     * 行为权重
     * VIEW=1, LIKE=3, FAVORITE=5, COMMENT=4, SHARE=2
     */
    private Integer weight;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

