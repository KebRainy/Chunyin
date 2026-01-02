package com.example.demo1.common.enums;

/**
 * 私信接收策略枚举
 */
public enum MessagePolicy {
    /**
     * 接受所有人的私信
     */
    ALL,
    
    /**
     * 仅接受关注者的私信
     */
    FOLLOWERS_ONLY,
    
    /**
     * 不接受任何私信
     */
    NONE
}

