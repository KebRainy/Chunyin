package com.example.demo1.dto.request;

import com.example.demo1.common.enums.WikiStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核Wiki页面请求
 */
@Data
public class ReviewWikiPageRequest {
    @NotNull(message = "审核状态不能为空")
    private WikiStatus status;
    
    /**
     * 拒绝原因（当状态为REJECTED时可选）
     */
    private String rejectReason;
}


