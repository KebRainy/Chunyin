package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.ReportRequest;
import com.example.demo1.dto.response.ContentReportVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户举报控制器
 * SF-12: 评论内容审核与举报
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 提交举报
     */
    @PostMapping
    public Result<Long> submitReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long reportId = reportService.submitReport(principal.getId(), request);
        return Result.success(reportId);
    }

    /**
     * 获取我的举报列表
     */
    @GetMapping("/my")
    public Result<List<ContentReportVO>> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<ContentReportVO> reports = reportService.getMyReports(principal.getId(), page, size);
        return Result.success(reports);
    }

    /**
     * 获取举报详情（仅限自己的举报）
     */
    @GetMapping("/{id}")
    public Result<ContentReportVO> getReportDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        ContentReportVO report = reportService.getReportDetail(id);
        // 验证是否是自己的举报
        if (!report.getReporter().getId().equals(principal.getId())) {
            throw new BusinessException(403, "无权查看此举报");
        }
        return Result.success(report);
    }
}

