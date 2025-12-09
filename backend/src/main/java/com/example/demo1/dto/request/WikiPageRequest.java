package com.example.demo1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WikiPageRequest {
    @NotBlank(message = "请输入标题")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过 500 个字符")
    private String summary;

    @NotBlank(message = "请输入正文内容")
    private String content;
}

