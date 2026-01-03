package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo1.common.enums.Gender;
import com.example.demo1.common.enums.MessagePolicy;
import com.example.demo1.common.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String email;

    @TableField(value = "role")
    private UserRole role = UserRole.USER;

    private Long avatarImageId;
    private String avatarUrl;
    private String bio;
    private Gender gender = Gender.SECRET;
    private LocalDate birthday;
    private Integer level = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isActive = true;
    
    /**
     * 禁言截止时间
     */
    private LocalDateTime muteUntil;
    
    /**
     * 私信接收策略
     */
    private MessagePolicy messagePolicy = MessagePolicy.ALL;
}
