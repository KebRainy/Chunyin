package com.example.demo1.dto.request;

import com.example.demo1.common.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理举报请求DTO（管理员使用）
 */
@Data
public class HandleReportRequest {
    
    @NotNull(message = "处理状态不能为空")
    private ReportStatus status;
    
    @Size(max = 500, message = "处理备注不能超过500字")
    private String handleNote;
    
    /**
     * 处理动作：DELETE(删除内容), BLOCK(屏蔽内容), WARN(警告用户), BAN(封禁用户), NONE(无操作)
     */
    private String action;
}

