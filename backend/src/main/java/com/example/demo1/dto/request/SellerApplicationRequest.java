package com.example.demo1.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SellerApplicationRequest {
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    @NotBlank(message = "身份证号不能为空")
    private String idCardNumber;
    
    private String licenseImageUrl;
}
