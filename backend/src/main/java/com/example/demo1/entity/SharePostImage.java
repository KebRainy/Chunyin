package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("share_post_image")
public class SharePostImage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sharePostId;
    private Long imageId;
    private Integer imageOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
