package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo1.common.enums.ReviewStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("external_link")
public class ExternalLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long beverageId;
    private Long sellerId;
    private String title;
    private String url;
    private String description;
    private BigDecimal price;
    private String platform;
    private ReviewStatus status = ReviewStatus.PENDING;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isActive = true;
}
