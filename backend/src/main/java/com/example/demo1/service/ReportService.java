package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.enums.ContentType;
import com.example.demo1.common.enums.ReportReason;
import com.example.demo1.common.enums.ReportStatus;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.HandleReportRequest;
import com.example.demo1.dto.request.ReportRequest;
import com.example.demo1.dto.response.ContentReportVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.*;
import com.example.demo1.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 举报服务
 * SF-12: 评论内容审核与举报
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ContentReportMapper reportMapper;
    private final SharePostCommentMapper postCommentMapper;
    private final BarReviewMapper barReviewMapper;
    private final SharePostMapper sharePostMapper;
    private final UserMapper userMapper;
    private final ContentModerationService moderationService;
    private final ObjectMapper objectMapper;

    /**
     * 提交举报
     */
    @Transactional
    public Long submitReport(Long reporterId, ReportRequest request) {
        // 检查是否已经举报过
        LambdaQueryWrapper<ContentReport> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ContentReport::getReporterId, reporterId)
                .eq(ContentReport::getContentType, request.getContentType())
                .eq(ContentReport::getContentId, request.getContentId())
                .ne(ContentReport::getStatus, ReportStatus.DISMISSED);
        
        if (reportMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("您已经举报过该内容");
        }

        // 获取被举报内容信息
        ContentInfo contentInfo = getContentInfo(request.getContentType(), request.getContentId());
        if (contentInfo == null) {
            throw new BusinessException("被举报内容不存在");
        }

        // 自动审核
        ContentModerationService.ModerationResult moderationResult = 
                moderationService.moderate(contentInfo.content);

        // 计算风险等级
        int riskLevel = calculateRiskLevel(request.getReason(), moderationResult);

        // 创建举报记录
        ContentReport report = new ContentReport();
        report.setReporterId(reporterId);
        report.setContentType(request.getContentType());
        report.setContentId(request.getContentId());
        report.setContentAuthorId(contentInfo.authorId);
        report.setContentSnapshot(contentInfo.content);
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setRiskLevel(riskLevel);

        // 根据风险等级设置初始状态
        if (riskLevel >= 80) {
            // 高风险：自动触发审核流程
            report.setStatus(ReportStatus.UNDER_REVIEW);
            // 自动屏蔽高风险内容
            blockContent(request.getContentType(), request.getContentId());
        } else if (riskLevel >= 50) {
            // 中等风险：待人工审核
            report.setStatus(ReportStatus.PENDING);
        } else {
            // 低风险：待处理
            report.setStatus(ReportStatus.PENDING);
        }

        // 保存自动审核结果
        try {
            report.setAutoModerationResult(objectMapper.writeValueAsString(Map.of(
                    "level", moderationResult.getLevel().name(),
                    "violations", moderationResult.getViolations()
            )));
        } catch (Exception e) {
            log.warn("序列化审核结果失败", e);
        }

        reportMapper.insert(report);
        
        log.info("用户 {} 举报了 {} #{}, 风险等级: {}", 
                reporterId, request.getContentType(), request.getContentId(), riskLevel);

        return report.getId();
    }

    /**
     * 获取用户的举报列表
     */
    public List<ContentReportVO> getMyReports(Long userId, int page, int size) {
        Page<ContentReport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ContentReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentReport::getReporterId, userId)
                .orderByDesc(ContentReport::getCreatedAt);

        Page<ContentReport> result = reportMapper.selectPage(pageParam, wrapper);
        return convertToVOList(result.getRecords());
    }

    /**
     * 获取待处理的举报列表（管理员）
     */
    public PageResult<ContentReportVO> getPendingReports(int page, int size) {
        Page<ContentReport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ContentReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ContentReport::getStatus, ReportStatus.PENDING, ReportStatus.UNDER_REVIEW)
                .orderByDesc(ContentReport::getRiskLevel)
                .orderByAsc(ContentReport::getCreatedAt);

        Page<ContentReport> result = reportMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getTotal(), page, size, convertToVOList(result.getRecords()));
    }

    /**
     * 获取所有举报列表（管理员）
     */
    public PageResult<ContentReportVO> getAllReports(int page, int size, ReportStatus status) {
        Page<ContentReport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ContentReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ContentReport::getStatus, status);
        }
        wrapper.orderByDesc(ContentReport::getCreatedAt);

        Page<ContentReport> result = reportMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getTotal(), page, size, convertToVOList(result.getRecords()));
    }

    /**
     * 获取高风险举报列表（管理员）
     */
    public PageResult<ContentReportVO> getHighRiskReports(int page, int size, int minRiskLevel) {
        Page<ContentReport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ContentReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ContentReport::getRiskLevel, minRiskLevel)
                .in(ContentReport::getStatus, ReportStatus.PENDING, ReportStatus.UNDER_REVIEW)
                .orderByDesc(ContentReport::getRiskLevel)
                .orderByAsc(ContentReport::getCreatedAt);

        Page<ContentReport> result = reportMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getTotal(), page, size, convertToVOList(result.getRecords()));
    }

    /**
     * 处理举报（管理员）
     */
    @Transactional
    public void handleReport(Long reportId, Long handlerId, HandleReportRequest request) {
        ContentReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }

        // 更新举报状态
        LambdaUpdateWrapper<ContentReport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ContentReport::getId, reportId)
                .set(ContentReport::getStatus, request.getStatus())
                .set(ContentReport::getHandlerId, handlerId)
                .set(ContentReport::getHandleNote, request.getHandleNote())
                .set(ContentReport::getHandleAction, request.getAction())
                .set(ContentReport::getHandledAt, LocalDateTime.now());

        reportMapper.update(null, wrapper);

        // 根据处理动作执行相应操作
        if (request.getAction() != null) {
            executeAction(report, request.getAction());
        }

        log.info("管理员 {} 处理了举报 #{}, 状态: {}, 动作: {}", 
                handlerId, reportId, request.getStatus(), request.getAction());
    }

    /**
     * 批量处理举报（管理员）
     */
    @Transactional
    public void batchHandleReports(List<Long> reportIds, Long handlerId, HandleReportRequest request) {
        for (Long reportId : reportIds) {
            try {
                handleReport(reportId, handlerId, request);
            } catch (Exception e) {
                log.error("批量处理举报失败, reportId: {}", reportId, e);
            }
        }
    }

    /**
     * 获取举报详情
     */
    public ContentReportVO getReportDetail(Long reportId) {
        ContentReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        return convertToVO(report);
    }

    /**
     * 获取举报统计（管理员）
     */
    public Map<String, Object> getReportStats() {
        // 统计各状态数量
        long pendingCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getStatus, ReportStatus.PENDING));
        long underReviewCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getStatus, ReportStatus.UNDER_REVIEW));
        long confirmedCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getStatus, ReportStatus.CONFIRMED));
        long dismissedCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getStatus, ReportStatus.DISMISSED));
        long processedCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getStatus, ReportStatus.PROCESSED));
        
        // 高风险举报数量
        long highRiskCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .ge(ContentReport::getRiskLevel, 70)
                .in(ContentReport::getStatus, ReportStatus.PENDING, ReportStatus.UNDER_REVIEW));
        
        // 今日新增举报
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayCount = reportMapper.selectCount(new LambdaQueryWrapper<ContentReport>()
                .ge(ContentReport::getCreatedAt, today));
        
        return Map.of(
                "pending", pendingCount,
                "underReview", underReviewCount,
                "confirmed", confirmedCount,
                "dismissed", dismissedCount,
                "processed", processedCount,
                "highRisk", highRiskCount,
                "todayNew", todayCount,
                "total", pendingCount + underReviewCount + confirmedCount + dismissedCount + processedCount
        );
    }

    /**
     * 禁言用户（管理员）
     */
    @Transactional
    public void muteUser(Long userId, int days, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        LocalDateTime muteUntil = LocalDateTime.now().plusDays(days);
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getMuteUntil, muteUntil);
        userMapper.update(null, wrapper);
        
        log.info("用户 {} 被禁言至 {}, 原因: {}", userId, muteUntil, reason);
    }

    /**
     * 封禁用户（管理员）
     */
    @Transactional
    public void banUser(Long userId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getIsActive, false);
        userMapper.update(null, wrapper);
        
        log.info("用户 {} 被封禁, 原因: {}", userId, reason);
    }

    /**
     * 获取内容信息
     */
    private ContentInfo getContentInfo(ContentType type, Long contentId) {
        switch (type) {
            case POST_COMMENT:
                SharePostComment comment = postCommentMapper.selectById(contentId);
                if (comment != null) {
                    return new ContentInfo(comment.getContent(), comment.getUserId());
                }
                break;
            case BAR_REVIEW:
                BarReview review = barReviewMapper.selectById(contentId);
                if (review != null) {
                    return new ContentInfo(review.getContent(), review.getUserId());
                }
                break;
            case POST:
                SharePost post = sharePostMapper.selectById(contentId);
                if (post != null) {
                    return new ContentInfo(post.getContent(), post.getUserId());
                }
                break;
            default:
                break;
        }
        return null;
    }

    /**
     * 计算风险等级
     */
    private int calculateRiskLevel(ReportReason reason, ContentModerationService.ModerationResult moderationResult) {
        int baseLevel = 0;
        
        // 根据举报原因设置基础风险
        switch (reason) {
            case ILLEGAL:
            case PORNOGRAPHY:
                baseLevel = 60;
                break;
            case FRAUD:
            case ABUSE:
                baseLevel = 50;
                break;
            case HARASSMENT:
            case MISINFORMATION:
                baseLevel = 40;
                break;
            case SPAM:
                baseLevel = 30;
                break;
            default:
                baseLevel = 20;
        }

        // 根据自动审核结果调整
        if (moderationResult.isRejected()) {
            baseLevel += 30;
        } else if (moderationResult.needsReview()) {
            baseLevel += 15;
        }

        // 确保在0-100范围内
        return Math.min(100, Math.max(0, baseLevel));
    }

    /**
     * 屏蔽内容
     */
    private void blockContent(ContentType type, Long contentId) {
        switch (type) {
            case POST_COMMENT:
                SharePostComment comment = postCommentMapper.selectById(contentId);
                if (comment != null) {
                    // 标记评论为已屏蔽（通过删除或软删除）
                    postCommentMapper.deleteById(contentId);
                    log.info("已屏蔽评论 #{}", contentId);
                }
                break;
            case BAR_REVIEW:
                LambdaUpdateWrapper<BarReview> reviewWrapper = new LambdaUpdateWrapper<>();
                reviewWrapper.eq(BarReview::getId, contentId)
                        .set(BarReview::getIsActive, false);
                barReviewMapper.update(null, reviewWrapper);
                log.info("已屏蔽酒吧评价 #{}", contentId);
                break;
            case POST:
                // 可以选择删除或标记
                sharePostMapper.deleteById(contentId);
                log.info("已屏蔽动态 #{}", contentId);
                break;
            default:
                break;
        }
    }

    /**
     * 执行处理动作
     */
    private void executeAction(ContentReport report, String action) {
        switch (action.toUpperCase()) {
            case "DELETE":
            case "BLOCK":
                blockContent(report.getContentType(), report.getContentId());
                break;
            case "WARN":
                // 发送警告消息给用户（可以通过私信系统）
                log.info("向用户 {} 发送警告", report.getContentAuthorId());
                break;
            case "MUTE_3":
                muteUser(report.getContentAuthorId(), 3, "违规内容");
                break;
            case "MUTE_7":
                muteUser(report.getContentAuthorId(), 7, "违规内容");
                break;
            case "MUTE_30":
                muteUser(report.getContentAuthorId(), 30, "违规内容");
                break;
            case "BAN":
                banUser(report.getContentAuthorId(), "严重违规");
                break;
            default:
                break;
        }
    }

    /**
     * 转换为VO列表
     */
    private List<ContentReportVO> convertToVOList(List<ContentReport> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }

        // 获取所有相关用户ID
        List<Long> userIds = reports.stream()
                .flatMap(r -> Stream.of(r.getReporterId(), r.getContentAuthorId(), r.getHandlerId()))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reports.stream()
                .map(r -> convertToVO(r, userMap))
                .collect(Collectors.toList());
    }

    private ContentReportVO convertToVO(ContentReport report) {
        List<Long> userIds = Stream.of(report.getReporterId(), report.getContentAuthorId(), report.getHandlerId())
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return convertToVO(report, userMap);
    }

    private ContentReportVO convertToVO(ContentReport report, Map<Long, User> userMap) {
        return ContentReportVO.builder()
                .id(report.getId())
                .reporter(buildSimpleUser(userMap.get(report.getReporterId())))
                .contentType(report.getContentType())
                .contentId(report.getContentId())
                .contentAuthor(buildSimpleUser(userMap.get(report.getContentAuthorId())))
                .contentSnapshot(report.getContentSnapshot())
                .reason(report.getReason())
                .reasonDescription(report.getReason() != null ? report.getReason().getDescription() : null)
                .description(report.getDescription())
                .status(report.getStatus())
                .statusDescription(report.getStatus() != null ? report.getStatus().getDescription() : null)
                .riskLevel(report.getRiskLevel())
                .autoModerationResult(report.getAutoModerationResult())
                .handler(buildSimpleUser(userMap.get(report.getHandlerId())))
                .handleNote(report.getHandleNote())
                .handleAction(report.getHandleAction())
                .handledAt(report.getHandledAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private SimpleUserVO buildSimpleUser(User user) {
        if (user == null) {
            return null;
        }
        return SimpleUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();
    }

    /**
     * 内容信息内部类
     */
    private static class ContentInfo {
        final String content;
        final Long authorId;

        ContentInfo(String content, Long authorId) {
            this.content = content;
            this.authorId = authorId;
        }
    }
}

