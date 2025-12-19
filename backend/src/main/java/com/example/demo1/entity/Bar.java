package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("bar")
public class Bar {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的申请ID
    private Long applicationId;

    private String name;
    private String address;
    private String province;
    private String city;
    private String district;
    
    // 经纬度（用于附近搜索）
    private Double latitude;
    private Double longitude;
    
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String contactPhone;
    private String description;
    
    // 主营酒类
    private String mainBeverages;
    
    // 酒吧所有者ID
    private Long ownerId;
    
    // 综合评分 (1-5分)
    private Double avgRating = 0.0;
    
    // 评价数量
    private Integer reviewCount = 0;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean isActive = true;
}

