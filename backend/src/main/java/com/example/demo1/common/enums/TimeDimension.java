package com.example.demo1.common.enums;

/**
 * 时间维度枚举
 * 用于排行榜的时间范围筛选
 */
public enum TimeDimension {
    DAY(1, "今日"),
    WEEK(7, "本周"),
    MONTH(30, "本月"),
    ALL(Integer.MAX_VALUE, "全部");

    private final int days;
    private final String description;

    TimeDimension(int days, String description) {
        this.days = days;
        this.description = description;
    }

    public int getDays() {
        return days;
    }

    public String getDescription() {
        return description;
    }
}

