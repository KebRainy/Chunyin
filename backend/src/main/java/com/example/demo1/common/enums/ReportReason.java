package com.example.demo1.common.enums;

import lombok.Getter;

/**
 * 举报原因枚举
 */
@Getter
public enum ReportReason {
    SPAM("垃圾广告"),
    ABUSE("辱骂攻击"),
    PORNOGRAPHY("色情低俗"),
    ILLEGAL("违法违规"),
    FRAUD("欺诈信息"),
    MISINFORMATION("虚假信息"),
    HARASSMENT("骚扰行为"),
    OTHER("其他原因");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}

