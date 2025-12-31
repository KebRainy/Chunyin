package com.example.demo1.common.enums;

import lombok.Getter;

/**
 * 举报状态枚举
 */
@Getter
public enum ReportStatus {
    PENDING("待处理"),
    UNDER_REVIEW("审核中"),
    CONFIRMED("已确认违规"),
    DISMISSED("已驳回"),
    PROCESSED("已处理");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }
}

