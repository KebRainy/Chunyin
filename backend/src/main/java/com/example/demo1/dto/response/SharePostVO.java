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
    private String ipAddressMasked;
    private LocalDateTime createdAt;
}

