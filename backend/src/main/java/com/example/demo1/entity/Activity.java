package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.example.demo1.common.enums.ActivityStatus;
import com.example.demo1.common.enums.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动实体
 */
@Data
@TableName(value = "activity", autoResultMap = true)
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起者ID
     */
    private Long organizerId;

    /**
     * 活动时间
     */
    private LocalDateTime activityTime;

    /**
     * 主要酒类ID（保留用于兼容）
     */
    private Long beverageId;

    /**
     * 酒类标签ID列表（JSON格式存储）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> alcoholIds;

    /**
     * 酒吧ID
     */
    private Long barId;

    /**
     * 参与人数上限
     */
    private Integer maxParticipants;

    /**
     * 备注
     */
    private String remark;

    /**
     * 审核状态
     */
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    /**
     * 审核人ID
     */
    private Long reviewedBy;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 活动状态
     */
    private ActivityStatus status = ActivityStatus.PENDING;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isActive = true;
}

