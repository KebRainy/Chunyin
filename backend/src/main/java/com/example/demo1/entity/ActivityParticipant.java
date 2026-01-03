package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动参与者实体
 */
@Data
@TableName("activity_participant")
public class ActivityParticipant {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 参与者ID
     */
    private Long userId;

    /**
     * 参与时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;

    /**
     * 是否已评价
     */
    private Boolean hasReviewed = false;
}

