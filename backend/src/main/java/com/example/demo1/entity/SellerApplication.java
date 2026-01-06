package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo1.common.enums.SellerApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seller_application")
public class SellerApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    
    private String realName;
    private String idCardNumber;
    
    private String licenseImageUrl;
    
    @TableField(value = "status")
    private SellerApplicationStatus status = SellerApplicationStatus.PENDING;
    
    private String reviewNote;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean isActive = true;
}
