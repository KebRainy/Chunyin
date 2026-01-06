package com.example.demo1.controller;

import com.example.demo1.common.enums.UserRole;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.ActivityBarReviewRequest;
import com.example.demo1.dto.request.CreateActivityRequest;
import com.example.demo1.dto.request.ReviewActivityRequest;
import com.example.demo1.dto.response.ActivityVO;
import com.example.demo1.dto.response.BarVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动控制器
 */
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 创建活动
     */
    @PostMapping
    public Result<Long> createActivity(@AuthenticationPrincipal UserPrincipal principal,
                                      @Valid @RequestBody CreateActivityRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long activityId = activityService.createActivity(request, principal.getId());
        return Result.success(activityId);
    }

    /**
     * 根据酒类标签推荐酒吧
     */
    @GetMapping("/bars/recommend")
    public Result<List<BarVO>> recommendBarsByAlcoholIds(
            @RequestParam(value = "alcoholIds") List<Long> alcoholIds,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "10") Integer limit) {
        // 参数验证
        if (alcoholIds == null || alcoholIds.isEmpty()) {
            throw new BusinessException(400, "请至少选择一个酒类标签");
        }
        // 过滤掉null值
        List<Long> validIds = alcoholIds.stream()
                .filter(id -> id != null)
                .toList();
        if (validIds.isEmpty()) {
            throw new BusinessException(400, "请至少选择一个有效的酒类标签");
        }
        List<BarVO> bars = activityService.recommendBarsByAlcoholIds(validIds, latitude, longitude, limit);
        return Result.success(bars);
    }

    /**
     * 根据酒类推荐酒吧（保留用于兼容）
     * @deprecated 使用 recommendBarsByAlcoholIds 代替
     */
    @Deprecated
    @GetMapping("/bars/recommend-by-beverage")
    public Result<List<BarVO>> recommendBarsByBeverage(
            @RequestParam Long beverageId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<BarVO> bars = activityService.recommendBarsByBeverage(beverageId, latitude, longitude, limit);
        return Result.success(bars);
    }

    /**
     * 搜索酒吧（模糊搜索）
     */
    @GetMapping("/bars/search")
    public Result<List<BarVO>> searchBars(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        List<BarVO> bars = activityService.searchBars(keyword, limit);
        return Result.success(bars);
    }

    /**
     * 获取我发起的活动
     */
    @GetMapping("/my-created")
    public Result<PageResult<ActivityVO>> getMyCreatedActivities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        PageResult<ActivityVO> result = activityService.getMyCreatedActivities(principal.getId(), page, size);
        return Result.success(result);
    }

    /**
     * 获取推荐的活动
     * @param timeRange 时间范围：THREE_DAYS(最近三天), ONE_MONTH(最近一个月), ONE_YEAR(最近一年), ALL(全部)
     */
    @GetMapping("/recommended")
    public Result<PageResult<ActivityVO>> getRecommendedActivities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long barId,
            @RequestParam(required = false) Long beverageId,
            @RequestParam(required = false) String timeRange,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = principal != null ? principal.getId() : null;
        PageResult<ActivityVO> result = activityService.getRecommendedActivities(userId, barId, beverageId, timeRange, page, size);
        return Result.success(result);
    }

    /**
     * 获取我参与的活动
     */
    @GetMapping("/my-participated")
    public Result<PageResult<ActivityVO>> getMyParticipatedActivities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        PageResult<ActivityVO> result = activityService.getMyParticipatedActivities(principal.getId(), page, size);
        return Result.success(result);
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public Result<ActivityVO> getActivityById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        ActivityVO activity = activityService.getActivityById(id, userId);
        return Result.success(activity);
    }

    /**
     * 参与活动
     */
    @PostMapping("/{id}/join")
    public Result<Void> joinActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        activityService.joinActivity(id, principal.getId());
        return Result.success();
    }

    /**
     * 取消参与活动
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelJoinActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        activityService.cancelJoinActivity(id, principal.getId());
        return Result.success();
    }

    /**
     * 取消活动（发起者）
     */
    @PostMapping("/{id}/cancel-activity")
    public Result<Void> cancelActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        activityService.cancelActivity(id, principal.getId());
        return Result.success();
    }

    /**
     * 审核活动（管理员）
     */
    @PostMapping("/{id}/review")
    public Result<Void> reviewActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewActivityRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserRole.ADMIN.equals(principal.getRole())) {
            throw new BusinessException(403, "需要管理员权限");
        }
        activityService.reviewActivity(id, request, principal.getId());
        return Result.success();
    }

    /**
     * 获取待审核的活动列表（管理员）
     */
    @GetMapping("/pending")
    public Result<PageResult<ActivityVO>> getPendingActivities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserRole.ADMIN.equals(principal.getRole())) {
            throw new BusinessException(403, "需要管理员权限");
        }
        PageResult<ActivityVO> result = activityService.getPendingActivities(page, size);
        return Result.success(result);
    }

    /**
     * 获取活动参与者列表
     */
    @GetMapping("/{id}/participants")
    public Result<List<com.example.demo1.dto.response.SimpleUserVO>> getActivityParticipants(
            @PathVariable Long id) {
        List<com.example.demo1.dto.response.SimpleUserVO> participants = activityService.getActivityParticipants(id);
        return Result.success(participants);
    }

    /**
     * 活动结束后评价酒吧
     */
    @PostMapping("/{id}/review-bar")
    public Result<Void> reviewActivityBar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ActivityBarReviewRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        activityService.reviewActivityBar(id, request, principal.getId());
        return Result.success();
    }
}

