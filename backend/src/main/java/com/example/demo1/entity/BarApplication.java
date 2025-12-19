package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo1.common.enums.BarStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("bar_application")
public class BarApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String address;
    private String province;
    private String city;
    private String district;
    
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String contactPhone;
    private String description;
    private String mainBeverages;
    
    private Long applicantId;
    
    @TableField(value = "status")
    private BarStatus status = BarStatus.PENDING;
    
    private String reviewNote;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean isActive = true;
}

