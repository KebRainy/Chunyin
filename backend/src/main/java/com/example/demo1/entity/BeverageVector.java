package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 酒类向量存储实体
 */
@Data
@TableName("beverage_vector")
public class BeverageVector {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long beverageId;
    private String textContent;
    private String vector; // JSON格式存储向量
    private String sourceType; // database/external
    private Integer chunkIndex;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


