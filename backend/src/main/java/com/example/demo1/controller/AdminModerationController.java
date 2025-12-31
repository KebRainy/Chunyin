package com.example.demo1.controller;

import com.example.demo1.common.enums.ReportStatus;
import com.example.demo1.common.enums.UserRole;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.HandleReportRequest;
import com.example.demo1.dto.response.ContentReportVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员内容审核控制器
 * SF-12: 评论内容审核与举报
 */
@RestController
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final ReportService reportService;

    /**
     * 验证管理员权限
     */
    private void checkAdminPermission(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserRole.ADMIN.equals(principal.getRole())) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    /**
     * 获取举报统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getReportStats(@AuthenticationPrincipal UserPrincipal principal) {
        checkAdminPermission(principal);
        return Result.success(reportService.getReportStats());
    }

    /**
     * 获取待处理举报列表
     */
    @GetMapping("/reports/pending")
    public Result<PageResult<ContentReportVO>> getPendingReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdminPermission(principal);
        return Result.success(reportService.getPendingReports(page, size));
    }

    /**
     * 获取高风险举报列表
     */
    @GetMapping("/reports/high-risk")
    public Result<PageResult<ContentReportVO>> getHighRiskReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "70") int minRiskLevel) {
        checkAdminPermission(principal);
        return Result.success(reportService.getHighRiskReports(page, size, minRiskLevel));
    }

    /**
     * 获取所有举报列表（支持按状态筛选）
     */
    @GetMapping("/reports")
    public Result<PageResult<ContentReportVO>> getAllReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReportStatus status) {
        checkAdminPermission(principal);
        return Result.success(reportService.getAllReports(page, size, status));
    }

    /**
     * 获取举报详情
     */
    @GetMapping("/reports/{id}")
    public Result<ContentReportVO> getReportDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        checkAdminPermission(principal);
        return Result.success(reportService.getReportDetail(id));
    }

    /**
     * 处理举报
     */
    @PostMapping("/reports/{id}/handle")
    public Result<Void> handleReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody HandleReportRequest request) {
        checkAdminPermission(principal);
        reportService.handleReport(id, principal.getId(), request);
        return Result.success();
    }

    /**
     * 批量处理举报
     */
    @PostMapping("/reports/batch-handle")
    public Result<Void> batchHandleReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        checkAdminPermission(principal);
        
        @SuppressWarnings("unchecked")
        List<Long> reportIds = ((List<Number>) body.get("reportIds")).stream()
                .map(Number::longValue)
                .toList();
        
        HandleReportRequest request = new HandleReportRequest();
        request.setStatus(ReportStatus.valueOf((String) body.get("status")));
        request.setHandleNote((String) body.get("handleNote"));
        request.setAction((String) body.get("action"));
        
        reportService.batchHandleReports(reportIds, principal.getId(), request);
        return Result.success();
    }

    /**
     * 禁言用户
     */
    @PostMapping("/users/{userId}/mute")
    public Result<Void> muteUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        checkAdminPermission(principal);
        
        int days = ((Number) body.getOrDefault("days", 7)).intValue();
        String reason = (String) body.getOrDefault("reason", "违规内容");
        
        reportService.muteUser(userId, days, reason);
        return Result.success();
    }

    /**
     * 封禁用户
     */
    @PostMapping("/users/{userId}/ban")
    public Result<Void> banUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        checkAdminPermission(principal);
        
        String reason = (String) body.getOrDefault("reason", "严重违规");
        
        reportService.banUser(userId, reason);
        return Result.success();
    }
}

