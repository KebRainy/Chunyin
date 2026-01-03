package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 推荐用户VO
 */
@Data
@Builder
public class RecommendedUserVO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    /**
     * 推荐原因
     * 例如："可能同关注的人"、"常看的贴子的发帖人"
     */
    private String reason;
    /**
     * 是否已关注
     */
    private Boolean following;
}

