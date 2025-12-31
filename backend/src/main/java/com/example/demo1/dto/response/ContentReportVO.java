package com.example.demo1.dto.response;

import com.example.demo1.common.enums.ContentType;
import com.example.demo1.common.enums.ReportReason;
import com.example.demo1.common.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容举报响应VO
 */
@Data
@Builder
public class ContentReportVO {
    
    private Long id;
    
    /**
     * 举报人信息
     */
    private SimpleUserVO reporter;
    
    /**
     * 内容类型
     */
    private ContentType contentType;
    
    /**
     * 内容ID
     */
    private Long contentId;
    
    /**
     * 内容作者信息
     */
    private SimpleUserVO contentAuthor;
    
    /**
     * 内容快照
     */
    private String contentSnapshot;
    
    /**
     * 举报原因
     */
    private ReportReason reason;
    
    /**
     * 举报原因描述
     */
    private String reasonDescription;
    
    /**
     * 详细说明
     */
    private String description;
    
    /**
     * 举报状态
     */
    private ReportStatus status;
    
    /**
     * 状态描述
     */
    private String statusDescription;
    
    /**
     * 风险等级
     */
    private Integer riskLevel;
    
    /**
     * 自动审核结果
     */
    private String autoModerationResult;
    
    /**
     * 处理人信息
     */
    private SimpleUserVO handler;
    
    /**
     * 处理备注
     */
    private String handleNote;
    
    /**
     * 处理动作
     */
    private String handleAction;
    
    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

