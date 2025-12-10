package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("beverage")
public class Beverage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String nameEn;
    private String type;
    private String origin;
    private BigDecimal alcoholContent;
    private String volume;
    private String description;
    private String ingredients;
    private String tasteNotes;
    private Long coverImageId;
    private BigDecimal rating = BigDecimal.ZERO;
    private Integer ratingCount = 0;
    private Integer viewCount = 0;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isActive = true;
}
