package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationSummaryVO {
    private SimpleUserVO peer;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
}

