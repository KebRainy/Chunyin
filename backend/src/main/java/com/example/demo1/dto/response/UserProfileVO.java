package com.example.demo1.dto.response;

import com.example.demo1.common.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileVO {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;
    private Long followerCount;
    private Long followingCount;
    private Boolean following;
    private Boolean self;
}

