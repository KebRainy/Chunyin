package com.example.demo1.controller;

import com.example.demo1.common.response.Result;
import com.example.demo1.common.enums.SellerApplicationStatus;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.dto.request.SellerApplicationRequest;
import com.example.demo1.dto.response.SellerApplicationVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * 提交商家认证申请
     */
    @PostMapping("/applications")
    public Result<Long> submitApplication(@RequestBody @Valid SellerApplicationRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long applicationId = sellerService.submitApplication(request, principal.getId());
        return Result.success(applicationId);
    }

    /**
     * 获取当前用户的申请记录
     */
    @GetMapping("/applications/my")
    public Result<List<SellerApplicationVO>> getMyApplications(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<SellerApplicationVO> applications = sellerService.getMyApplications(principal.getId());
        return Result.success(applications);
    }

    /**
     * 获取待审核的申请列表（管理员）
     */
    @GetMapping("/applications/pending")
    public Result<List<SellerApplicationVO>> getPendingApplications(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        // TODO: 添加管理员权限校验
        List<SellerApplicationVO> applications = sellerService.getPendingApplications();
        return Result.success(applications);
    }

    /**
     * 审核申请（管理员）
     */
    @PutMapping("/applications/{id}/review")
    public Result<Void> reviewApplication(@PathVariable Long id,
                                          @RequestParam SellerApplicationStatus status,
                                          @RequestParam(required = false) String reviewNote,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        // TODO: 添加管理员权限校验
        sellerService.reviewApplication(id, status, reviewNote, principal.getId());
        return Result.success();
    }
}
