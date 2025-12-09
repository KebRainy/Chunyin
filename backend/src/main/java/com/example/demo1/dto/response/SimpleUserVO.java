package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleUserVO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
}

