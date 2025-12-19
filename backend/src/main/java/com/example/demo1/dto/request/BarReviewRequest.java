package com.example.demo1.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BarReviewRequest {
    @NotNull(message = "酒吧ID不能为空")
    private Long barId;
    
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1分")
    @Max(value = 5, message = "评分最高为5分")
    private Integer rating;
    
    private String content;
}

