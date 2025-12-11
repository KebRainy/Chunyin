package com.example.demo1.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AnswerDailyQuestionRequest {
    @Min(value = 0, message = "选项索引不正确")
    @Max(value = 3, message = "选项索引不正确")
    private int optionIndex;
}
