package com.example.demo1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class BarRegisterRequest {
    @NotBlank(message = "酒吧名称不能为空")
    private String name;
    
    @NotBlank(message = "详细地址不能为空")
    private String address;
    
    @NotBlank(message = "省份不能为空")
    private String province;
    
    @NotBlank(message = "城市不能为空")
    private String city;
    
    private String district;
    
    private LocalTime openingTime;
    private LocalTime closingTime;
    
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;
    
    private String description;
    private String mainBeverages;
}

