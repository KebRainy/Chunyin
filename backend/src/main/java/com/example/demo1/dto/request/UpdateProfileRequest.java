package com.example.demo1.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;

    @Size(max = 500, message = "个人简介不能超过 500 个字符")
    private String bio;
}

