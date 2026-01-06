package com.example.demo1.dto.response;

import com.example.demo1.common.enums.SellerApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SellerApplicationVO {
    private Long id;
    private Long userId;
    private String realName;
    private String idCardNumber;
    private String licenseImageUrl;
    private SellerApplicationStatus status;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
