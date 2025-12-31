package com.example.demo1.common.enums;

import lombok.Getter;

/**
 * 评论状态枚举
 */
@Getter
public enum CommentStatus {
    /**
     * 正常显示
     */
    NORMAL,
    
    /**
     * 待审核
     */
    PENDING_REVIEW,
    
    /**
     * 已屏蔽（违规）
     */
    BLOCKED,
    
    /**
     * 已删除
     */
    DELETED
}

