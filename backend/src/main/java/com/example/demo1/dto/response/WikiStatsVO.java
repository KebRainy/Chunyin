package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WikiStatsVO {
    private Long entryCount;
    private Long editCount;
    private Long contributorCount;
}

