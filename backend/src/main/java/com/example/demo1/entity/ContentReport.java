package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo1.common.enums.ContentType;
import com.example.demo1.common.enums.ReportReason;
import com.example.demo1.common.enums.ReportStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容举报实体
 */
@Data
@TableName("content_report")
public class ContentReport {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 举报人ID
     */
    private Long reporterId;
    
    /**
     * 被举报内容类型
     */
    private ContentType contentType;
    
    /**
     * 被举报内容ID
     */
    private Long contentId;
    
    /**
     * 被举报内容作者ID
     */
    private Long contentAuthorId;
    
    /**
     * 被举报内容快照
     */
    private String contentSnapshot;
    
    /**
     * 举报原因
     */
    private ReportReason reason;
    
    /**
     * 详细说明
     */
    private String description;
    
    /**
     * 举报状态
     */
    private ReportStatus status;
    
    /**
     * 自动审核结果(JSON)
     */
    private String autoModerationResult;
    
    /**
     * 风险等级(0-100)
     */
    private Integer riskLevel;
    
    /**
     * 处理人ID
     */
    private Long handlerId;
    
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
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

