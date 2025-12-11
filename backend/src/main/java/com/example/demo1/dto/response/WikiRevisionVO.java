package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WikiRevisionVO {
    private Long id;
    private String editorName;
    private String summary;
    private LocalDateTime createdAt;
}

