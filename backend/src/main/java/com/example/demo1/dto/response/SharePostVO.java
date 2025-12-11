package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SharePostVO {
    private Long id;
    private SimpleUserVO author;
    private String content;
    private List<String> imageUrls;
    private String location;
    private List<String> tags;
    private String ipAddressMasked;
    private String ipRegion;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Boolean liked;
    private Boolean favorited;
    private LocalDateTime createdAt;
}
