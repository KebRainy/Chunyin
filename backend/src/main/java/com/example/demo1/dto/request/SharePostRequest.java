package com.example.demo1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SharePostRequest {
    @NotBlank(message = "请输入分享内容")
    @Size(max = 2000, message = "分享内容不能超过 2000 个字符")
    private String content;

    private List<String> imageUrls = new ArrayList<>();
    private String location;
}

