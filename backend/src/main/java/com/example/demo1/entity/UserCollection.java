package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo1.common.enums.CollectionTargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_collection")
public class UserCollection {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private CollectionTargetType targetType;
    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

