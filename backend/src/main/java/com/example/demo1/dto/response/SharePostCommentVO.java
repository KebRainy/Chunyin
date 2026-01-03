package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class SharePostCommentVO {
    private Long id;
    private String content;
    private SimpleUserVO author;
    private LocalDateTime createdAt;
    private Long parentId;
    private SimpleUserVO replyToUser; // 被回复的用户信息
    private List<SharePostCommentVO> replies;

    public List<SharePostCommentVO> safeReplies() {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        return replies;
    }
}

