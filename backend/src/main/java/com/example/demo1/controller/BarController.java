package com.example.demo1.controller;

import com.example.demo1.common.enums.BarSortStrategy;
import com.example.demo1.common.enums.BarStatus;
import com.example.demo1.common.enums.UserRole;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.BarRegisterRequest;
import com.example.demo1.dto.request.BarReviewRequest;
import com.example.demo1.dto.response.BarReviewVO;
import com.example.demo1.dto.response.BarApplicationVO;
import com.example.demo1.dto.response.BarVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.BarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bars")
@RequiredArgsConstructor
public class BarController {

    private final BarService barService;

    /**
     * 注册酒吧（提交申请）
     */
    @PostMapping("/register")
    public Result<Long> registerBar(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody BarRegisterRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        
        // 检查用户是否为商家
        if (principal.getRole() != UserRole.SELLER && principal.getRole() != UserRole.ADMIN) {
             throw new BusinessException(403, "只有认证商家才能注册酒吧，请先进行商家认证");
        }
        
        Long barId = barService.registerBar(request, principal.getId());
        return Result.success(barId);
    }

    /**
     * 获取酒吧详情
     */
    @GetMapping("/{id}")
    public Result<BarVO> getBarById(@PathVariable Long id) {
        BarVO bar = barService.getBarById(id);
        return Result.success(bar);
    }

    /**
     * 搜索附近的酒吧（SF-11: 附近最佳酒吧排序）
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param radiusKm 搜索半径（公里），默认10公里
     * @param sortStrategy 排序策略：COMPREHENSIVE(综合), DISTANCE_FIRST(距离优先), 
     *                     RATING_FIRST(评分优先), DISTANCE_ONLY(仅距离), RATING_ONLY(仅评分)
     */
    @GetMapping("/nearby")
    public Result<List<BarVO>> searchNearbyBars(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm,
            @RequestParam(defaultValue = "COMPREHENSIVE") BarSortStrategy sortStrategy) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("请提供有效的经纬度坐标");
        }
        if (radiusKm == null || radiusKm <= 0) {
            throw new BusinessException("搜索半径必须大于0公里");
        }
        List<BarVO> bars = barService.searchNearbyBars(latitude, longitude, radiusKm, sortStrategy);
        return Result.success(bars);
    }

    /**
     * 按城市搜索酒吧
     */
    @GetMapping("/city/{city}")
    public Result<List<BarVO>> searchBarsByCity(@PathVariable String city) {
        List<BarVO> bars = barService.searchBarsByCity(city);
        return Result.success(bars);
    }

    /**
     * 按名称搜索酒吧
     */
    @GetMapping("/search")
    public Result<List<BarVO>> searchBarsByName(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "20") Integer limit) {
        // 如果没有提供name参数，返回所有酒吧（限制数量）
        if (name == null || name.trim().isEmpty()) {
            List<BarVO> allBars = barService.getAllBars(limit);
            return Result.success(allBars);
        }
        List<BarVO> bars = barService.searchBarsByName(name);
        // 限制返回数量
        if (bars.size() > limit) {
            bars = bars.subList(0, limit);
        }
        return Result.success(bars);
    }

    /**
     * 获取当前用户提交的酒吧申请列表
     */
    @GetMapping("/applications/my")
    public Result<List<BarApplicationVO>> getMyApplications(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<BarApplicationVO> applications = barService.getApplicationsByApplicant(principal.getId());
        return Result.success(applications);
    }

    /**
     * 获取待审核的酒吧申请列表（管理员功能）
     */
    @GetMapping("/applications/pending")
    public Result<List<BarApplicationVO>> getPendingApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        // TODO: 检查管理员权限
        List<BarApplicationVO> applications = barService.getPendingApplications();
        return Result.success(applications);
    }

    /**
     * 审核酒吧申请（管理员功能）
     */
    @PutMapping("/applications/{id}/review")
    public Result<Void> reviewBarApplication(@PathVariable Long id,
                                             @RequestParam BarStatus status,
                                             @RequestParam(required = false) String reviewNote,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        // TODO: 检查管理员权限
        barService.reviewBarApplication(id, status, reviewNote, principal.getId());
        return Result.success();
    }

    /**
     * 添加酒吧评价
     */
    @PostMapping("/reviews")
    public Result<Long> addBarReview(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody BarReviewRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long reviewId = barService.addBarReview(request, principal.getId());
        return Result.success(reviewId);
    }

    /**
     * 获取酒吧的评价列表
     */
    @GetMapping("/{barId}/reviews")
    public Result<List<BarReviewVO>> getBarReviews(@PathVariable Long barId) {
        List<BarReviewVO> reviews = barService.getBarReviews(barId);
        return Result.success(reviews);
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/reviews/{reviewId}")
    public Result<Void> deleteBarReview(@PathVariable Long reviewId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        barService.deleteBarReview(reviewId, principal.getId());
        return Result.success();
    }

    /**
     * 获取推荐的酒吧列表
     * 使用BarRankingAlgorithm算法进行排序
     * 
     * @param latitude 用户纬度
     * @param longitude 用户经度
     * @param limit 返回数量限制，默认5
     * @return 推荐的酒吧列表
     */
    @GetMapping("/recommend")
    public Result<List<BarVO>> getRecommendedBars(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "5") Integer limit) {
        // 如果未提供位置信息，返回空列表
        if (latitude == null || longitude == null) {
            return Result.success(List.of());
        }
        List<BarVO> bars = barService.getRecommendedBars(latitude, longitude, limit);
        return Result.success(bars);
    }

    /**
     * 获取商家拥有的酒吧列表（用于创建活动时选择）
     * 根据main_beverages包含的标签排序，包含标签的排在前面
     * 
     * @param principal 当前登录用户
     * @param alcoholIds 酒类标签ID列表（可选，用于排序）
     * @return 商家拥有的酒吧列表
     */
    @GetMapping("/my")
    public Result<List<BarVO>> getMyBars(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) List<Long> alcoholIds) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<BarVO> bars = barService.getBarsByOwnerId(principal.getId(), alcoholIds);
        return Result.success(bars);
    }
}

