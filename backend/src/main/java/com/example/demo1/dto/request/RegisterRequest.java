package com.example.demo1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "请输入用户名")
    @Size(min = 2, max = 50, message = "用户名长度需要在 2-50 之间")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 50, message = "密码长度需要在 6-50 之间")
    private String password;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "请输入邮箱地址")
    private String email;
}

