package com.example.demo1.dto.request;

import com.example.demo1.common.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @Size(max = 50, message = "用户名不要超过 50 个字符")
    private String username;

    @Email(message = "请输入有效的邮箱地址")
    private String email;

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;

    private Long avatarImageId;

    @Size(max = 500, message = "个人简介不能超过 500 个字符")
    private String bio;

    private Gender gender;

    private LocalDate birthday;
}
