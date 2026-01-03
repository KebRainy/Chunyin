package com.example.demo1.controller;

import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐控制器
 * 提供基于算法的内容推荐
 */
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendationService recommendationService;

    /**
     * 获取推荐动态
     * 基于协同过滤算法，结合用户行为偏好和标签匹配
     */
    @GetMapping("/posts")
    public Result<PageResult<SharePostVO>> getRecommendedPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Long currentUserId = principal != null ? principal.getId() : null;
        
        // 基于协同过滤算法的推荐
        List<SharePostVO> recommendedPosts = recommendationService.getRecommendedPosts(currentUserId, size);
        
        // 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, recommendedPosts.size());
        List<SharePostVO> pagedPosts = start < recommendedPosts.size() 
            ? recommendedPosts.subList(start, end)
            : List.of();
        
        PageResult<SharePostVO> result = new PageResult<>(
            (long) recommendedPosts.size(), 
            page, 
            size, 
            pagedPosts
        );
        
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
     * 基于标签相似度推荐
     */
    @GetMapping("/similar-posts/{postId}")
    public Result<java.util.List<SharePostVO>> getSimilarPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "5") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Long currentUserId = principal != null ? principal.getId() : null;
        
        // 基于标签相似度的推荐
        List<SharePostVO> similarPosts = recommendationService.getSimilarPosts(postId, currentUserId, size);
        
        return Result.success(similarPosts);
    }
}

