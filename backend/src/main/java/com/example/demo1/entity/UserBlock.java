package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户屏蔽表
 */
@Data
@TableName("user_block")
public class UserBlock {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 屏蔽者ID（谁屏蔽的）
     */
    private Long blockerId;

    /**
     * 被屏蔽者ID（被谁屏蔽）
     */
    private Long blockedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

