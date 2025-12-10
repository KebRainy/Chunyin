package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BeverageSummaryVO {
    private Long id;
    private String name;
    private String type;
    private String origin;
    private Long coverImageId;
    private BigDecimal rating;
}

