package com.example.demo1.dto.response;

import com.example.demo1.common.enums.WikiStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WikiPageVO {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String content;
    private WikiStatus status;
    private String lastEditorName;
    private LocalDateTime updatedAt;
}

