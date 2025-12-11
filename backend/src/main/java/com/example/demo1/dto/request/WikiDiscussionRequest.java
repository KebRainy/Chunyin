package com.example.demo1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WikiDiscussionRequest {

    @NotBlank(message = "讨论内容不能为空")
    @Size(max = 2000, message = "讨论内容不要超过 2000 个字符")
    private String content;
}
