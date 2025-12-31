package com.example.demo1.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 目标类型枚举
 * 用于标识用户行为的目标对象类型
 * 必须与数据库中的ENUM定义一致: POST, BEVERAGE, WIKI, BAR
 */
@Getter
public enum TargetType {
    POST("POST", "动态"),
    BEVERAGE("BEVERAGE", "饮品"),
    WIKI("WIKI", "Wiki页面"),
    BAR("BAR", "酒吧"),
    USER("USER", "用户"); // 注：USER不在数据库枚举中，仅用于其他场景

    @EnumValue
    private final String value;
    private final String description;

    TargetType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}

