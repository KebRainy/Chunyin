package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName(value = "share_post", autoResultMap = true)
public class SharePost {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls = new ArrayList<>();

    private String location;
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

