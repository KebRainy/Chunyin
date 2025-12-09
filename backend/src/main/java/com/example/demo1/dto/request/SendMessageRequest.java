package com.example.demo1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotBlank(message = "请输入消息内容")
    @Size(max = 1000, message = "消息内容不能超过 1000 个字符")
    private String content;
}

