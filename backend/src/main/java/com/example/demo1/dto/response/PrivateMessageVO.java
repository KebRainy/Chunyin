package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrivateMessageVO {
    private Long id;
    private SimpleUserVO sender;
    private SimpleUserVO receiver;
    private String content;
    private Boolean mine;
    private LocalDateTime createdAt;
}

