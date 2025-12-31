package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class BarVO {
    private Long id;
    private String name;
    private String address;
    private String province;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String contactPhone;
    private String description;
    private String mainBeverages;
    private Long ownerId;
    private Double avgRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    
    // 距离（公里），仅在附近搜索时返回
    private Double distance;
    
    // 综合评分（用于排序），仅在附近搜索时返回
    private Double score;
}
