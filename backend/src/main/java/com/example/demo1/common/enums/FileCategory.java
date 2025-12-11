package com.example.demo1.common.enums;

public enum FileCategory {
    GENERAL,
    POST,
    AVATAR,
    WIKI;

    public static FileCategory fromNullable(FileCategory category) {
        return category == null ? GENERAL : category;
    }
}
