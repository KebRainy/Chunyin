package com.example.demo1.dto.request;

import com.example.demo1.common.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核活动请求
 */
@Data
public class ReviewActivityRequest {
    @NotNull(message = "审核状态不能为空")
    private ReviewStatus status;

    /**
     * 拒绝原因（当状态为REJECTED时必填）
     */
    private String rejectReason;
}

