package com.example.demo1.dto.response;

import com.example.demo1.common.enums.ActivityStatus;
import com.example.demo1.common.enums.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动视图对象
 */
@Data
@Builder
public class ActivityVO {
    private Long id;
    private Long organizerId;
    private String organizerName;
    private String organizerAvatar;
    private LocalDateTime activityTime;
    private Long beverageId;
    private String beverageName;
    private Long barId;
    private String barName;
    private String barAddress;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String remark;
    private ReviewStatus reviewStatus;
    private ActivityStatus status;
    private LocalDateTime createdAt;
    
    /**
     * 当前用户是否已参与
     */
    private Boolean isParticipated;
    
    /**
     * 当前用户是否是发起者
     */
    private Boolean isOrganizer;
    
    /**
     * 活动是否已结束
     */
    private Boolean isFinished;
}

