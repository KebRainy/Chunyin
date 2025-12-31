package com.example.demo1.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户行为类型枚举
 * 必须与数据库中的ENUM定义一致: VIEW, LIKE, FAVORITE, COMMENT, SHARE
 * 权重: VIEW=1, LIKE=3, FAVORITE=5, COMMENT=4, SHARE=2
 */
@Getter
public enum BehaviorType {
    VIEW("VIEW", 1),
    LIKE("LIKE", 3),
    FAVORITE("FAVORITE", 5),
    COMMENT("COMMENT", 4),
    SHARE("SHARE", 2);

    @EnumValue
    private final String value;
    private final int weight;

    BehaviorType(String value, int weight) {
        this.value = value;
        this.weight = weight;
    }
}

