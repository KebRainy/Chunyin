package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class SearchResultVO {
    @Builder.Default
    private List<SharePostVO> posts = Collections.emptyList();

    @Builder.Default
    private List<BeverageSummaryVO> beverages = Collections.emptyList();

    @Builder.Default
    private List<SimpleUserVO> users = Collections.emptyList();
}

