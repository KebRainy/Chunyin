package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("beverage_image")
public class BeverageImage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long beverageId;
    private Long imageId;
    private Integer imageOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
