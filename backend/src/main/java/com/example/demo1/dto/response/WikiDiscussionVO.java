package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WikiDiscussionVO {
    private Long id;
    private SimpleUserVO author;
    private String content;
    private LocalDateTime createdAt;
}
