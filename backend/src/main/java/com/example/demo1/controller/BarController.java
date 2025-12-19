package com.example.demo1.controller;

import com.example.demo1.common.enums.BarStatus;
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
     * 搜索附近的酒吧
     * @param latitude 纬度
     * @param longitude 经度
     * @param radiusKm 搜索半径（公里），默认10公里
     */
    @GetMapping("/nearby")
    public Result<List<BarVO>> searchNearbyBars(@RequestParam Double latitude,
                                                @RequestParam Double longitude,
                                                @RequestParam(defaultValue = "10.0") Double radiusKm) {
        // #region agent log
        try {java.nio.file.Files.write(java.nio.file.Paths.get("e:\\学习\\大三\\软件工程\\期末项目\\Chunyin\\.cursor\\debug.log"),("{\"location\":\"BarController.searchNearbyBars\",\"message\":\"nearby search called\",\"data\":{\"latitude\":"+latitude+",\"longitude\":"+longitude+",\"radiusKm\":"+radiusKm+"},\"timestamp\":"+System.currentTimeMillis()+",\"sessionId\":\"debug-session\",\"runId\":\"nearby\",\"hypothesisId\":\"A\"}\n").getBytes(),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);} catch(Exception e){}
        // #endregion
        if (latitude == null || longitude == null) {
            throw new BusinessException("请提供有效的经纬度坐标");
        }
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new BusinessException("搜索半径必须在0-100公里之间");
        }
        List<BarVO> bars = barService.searchNearbyBars(latitude, longitude, radiusKm);
        // #region agent log
        try {java.nio.file.Files.write(java.nio.file.Paths.get("e:\\学习\\大三\\软件工程\\期末项目\\Chunyin\\.cursor\\debug.log"),("{\"location\":\"BarController.searchNearbyBars:result\",\"message\":\"nearby search result\",\"data\":{\"count\":"+bars.size()+"},\"timestamp\":"+System.currentTimeMillis()+",\"sessionId\":\"debug-session\",\"runId\":\"nearby\",\"hypothesisId\":\"B\"}\n").getBytes(),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);} catch(Exception e){}
        // #endregion
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
    public Result<List<BarVO>> searchBarsByName(@RequestParam String name) {
        List<BarVO> bars = barService.searchBarsByName(name);
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
}

