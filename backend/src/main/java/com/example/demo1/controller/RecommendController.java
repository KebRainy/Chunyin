package com.example.demo1.controller;

import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.SharePostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 推荐控制器
 * 提供基于算法的内容推荐
 */
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final SharePostService sharePostService;

    /**
     * 获取推荐动态
     * 当前实现：返回最新的热门动态
     */
    @GetMapping("/posts")
    public Result<PageResult<SharePostVO>> getRecommendedPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Long currentUserId = principal != null ? principal.getId() : null;
        
        // 简单实现：返回最新的热门动态作为推荐
        // 未来可以基于用户行为、偏好等实现更复杂的推荐算法
        PageResult<SharePostVO> result = sharePostService.listPosts(page, size, currentUserId);
        
        return Result.success(result);
    }

    /**
     * 获取推荐用户
     */
    @GetMapping("/users")
    public Result<PageResult<Object>> getRecommendedUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // TODO: 实现用户推荐逻辑
        return Result.success(new PageResult<>(0L, page, size, java.util.Collections.emptyList()));
    }

    /**
     * 获取推荐酒吧
     */
    @GetMapping("/bars")
    public Result<PageResult<Object>> getRecommendedBars(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // TODO: 实现酒吧推荐逻辑
        return Result.success(new PageResult<>(0L, page, size, java.util.Collections.emptyList()));
    }

    /**
     * 获取推荐酒饮
     */
    @GetMapping("/beverages")
    public Result<PageResult<Object>> getRecommendedBeverages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // TODO: 实现酒饮推荐逻辑
        return Result.success(new PageResult<>(0L, page, size, java.util.Collections.emptyList()));
    }

    /**
     * 获取相似动态
     */
    @GetMapping("/similar-posts/{postId}")
    public Result<java.util.List<SharePostVO>> getSimilarPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "5") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Long currentUserId = principal != null ? principal.getId() : null;
        
        // 简单实现：返回最新的几条动态作为相似推荐
        PageResult<SharePostVO> result = sharePostService.listPosts(1, size, currentUserId);
        
        return Result.success(result.getItems());
    }
}

