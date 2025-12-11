package com.example.demo1.dto.response;

import com.example.demo1.common.enums.CollectionTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FootprintVO {
    private Long id;
    private CollectionTargetType targetType;
    private Long targetId;
    private String title;
    private String summary;
    private String coverUrl;
    private LocalDateTime visitedAt;
}

