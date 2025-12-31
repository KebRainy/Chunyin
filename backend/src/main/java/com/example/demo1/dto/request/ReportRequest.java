package com.example.demo1.dto.request;

import com.example.demo1.common.enums.ContentType;
import com.example.demo1.common.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报请求DTO
 */
@Data
public class ReportRequest {
    
    @NotNull(message = "内容类型不能为空")
    private ContentType contentType;
    
    @NotNull(message = "内容ID不能为空")
    private Long contentId;
    
    @NotNull(message = "举报原因不能为空")
    private ReportReason reason;
    
    @Size(max = 500, message = "详细说明不能超过500字")
    private String description;
}

