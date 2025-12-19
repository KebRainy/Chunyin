package com.example.demo1.dto.response;

import com.example.demo1.common.enums.BarStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class BarApplicationVO {
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
    private BarStatus status;
    private String reviewNote;
    private LocalDateTime createdAt;
}

