package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo1.common.enums.FileCategory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image")
public class Image {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String uuid;
    private String fileName;
    private String mimeType;
    private Integer fileSize;
    private Long uploadedBy;
    private FileCategory category = FileCategory.GENERAL;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
