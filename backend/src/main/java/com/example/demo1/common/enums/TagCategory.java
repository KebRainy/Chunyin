package com.example.demo1.common.enums;

/**
 * 标签分类枚举
 * 必须与数据库中的ENUM定义一致
 */
public enum TagCategory {
    /**
     * 饮品类型
     */
    BEVERAGE_TYPE,
    
    /**
     * 口味相关
     */
    TASTE,
    
    /**
     * 场景相关
     */
    SCENE,
    
    /**
     * 地点相关
     */
    LOCATION,
    
    /**
     * 其他
     */
    OTHER,
    
    // 以下是额外支持的分类（不在数据库中，用于代码逻辑）
    
    /**
     * 话题标签（#话题#）
     * 注：数据库中会存储为OTHER
     */
    TOPIC,
    
    /**
     * 饮品相关（通用）
     * 注：数据库中会存储为BEVERAGE_TYPE
     */
    BEVERAGE,
    
    /**
     * 氛围相关
     * 注：数据库中会存储为SCENE
     */
    ATMOSPHERE
}
