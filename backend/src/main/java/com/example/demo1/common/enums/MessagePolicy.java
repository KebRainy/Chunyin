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
     * 仅接受我关注的人（我关注Ta才可给我发私信）
     */
    FOLLOWEES_ONLY,

    /**
     * 在我回复或关注Ta前，仅允许发送一条私信
     */
    LIMIT_ONE_BEFORE_REPLY_OR_FOLLOW,

    /**
     * 兼容旧值：仅接受关注我的人（Ta关注我才可给我发私信）
     */
    FOLLOWERS_ONLY,
    
    /**
     * 不接受任何私信
     */
    NONE
}

