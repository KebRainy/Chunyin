package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.SendMessageRequest;
import com.example.demo1.dto.response.ConversationSummaryVO;
import com.example.demo1.dto.response.PrivateMessageVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<List<ConversationSummaryVO>> listConversations(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(messageService.listConversations(principal.getId()));
    }

    @GetMapping("/with/{userId}")
    public Result<List<PrivateMessageVO>> conversation(@PathVariable Long userId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(messageService.listConversation(principal.getId(), userId));
    }

    @PostMapping("/with/{userId}")
    public Result<PrivateMessageVO> send(@PathVariable Long userId,
                                         @AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody SendMessageRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(messageService.sendMessage(principal.getId(), userId, request));
    }
}


