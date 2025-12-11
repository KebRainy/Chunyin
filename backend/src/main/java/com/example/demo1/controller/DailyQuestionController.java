package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.AnswerDailyQuestionRequest;
import com.example.demo1.dto.response.DailyQuestionVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.DailyQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/daily-question")
@RequiredArgsConstructor
public class DailyQuestionController {

    private final DailyQuestionService dailyQuestionService;

    @GetMapping("/today")
    public Result<DailyQuestionVO> today(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return Result.success(dailyQuestionService.getTodayQuestion(userId));
    }

    @PostMapping("/{id}/answer")
    public Result<DailyQuestionVO> answer(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody AnswerDailyQuestionRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(dailyQuestionService.answer(id, principal.getId(), request.getOptionIndex()));
    }
}
