package com.example.demo1.controller;

import com.example.demo1.common.enums.UserRole;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.response.RecommendedUserVO;
import com.example.demo1.dto.rag.RAGQueryRequest;
import com.example.demo1.dto.rag.RAGRecommendationResponse;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.rag.service.KnowledgeBaseService;
import com.example.demo1.rag.service.RAGRecommendationService;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.RecommendationService;
import com.example.demo1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推荐控制器
 * 提供基于算法的内容推荐
 */
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendationService recommendationService;
    private final UserService userService;
    private final RAGRecommendationService ragRecommendationService;
    private final KnowledgeBaseService knowledgeBaseService;

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
    public Result<List<RecommendedUserVO>> getRecommendedUsers(
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<RecommendedUserVO> recommendedUsers = userService.getRecommendedUsers(principal.getId(), size);
        return Result.success(recommendedUsers);
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
     * RAG智能推荐酒饮
     */
    @PostMapping("/beverages/rag")
    public Result<RAGRecommendationResponse> ragRecommendBeverages(
            @RequestBody RAGQueryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Long currentUserId = principal != null ? principal.getId() : null;
        
        RAGRecommendationResponse response = ragRecommendationService.recommend(request, currentUserId);
        
        return Result.success(response);
    }

    /**
     * 构建知识库（从数据库和Wiki）
     */
    @PostMapping("/admin/knowledge/build")
    public Result<Map<String, Object>> buildKnowledgeBase(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        if (principal == null || principal.getRole() != UserRole.ADMIN) {
            return Result.error(403, "需要管理员权限");
        }
        
        try {
            // 构建数据库知识库
            knowledgeBaseService.buildKnowledgeBaseFromDatabase();
            
            // 构建Wiki知识库
            knowledgeBaseService.buildKnowledgeBaseFromWiki();
            
            return Result.success(Map.of(
                    "message", "知识库构建成功",
                    "includes", List.of("database", "wiki")
            ));
        } catch (Exception e) {
            return Result.error(500, "知识库构建失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建Wiki知识库
     */
    @PostMapping("/admin/knowledge/build/wiki")
    public Result<Map<String, Object>> buildWikiKnowledgeBase(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        if (principal == null || principal.getRole() != UserRole.ADMIN) {
            return Result.error(403, "需要管理员权限");
        }
        
        try {
            knowledgeBaseService.buildKnowledgeBaseFromWiki();
            return Result.success(Map.of(
                    "message", "Wiki知识库构建成功"
            ));
        } catch (Exception e) {
            return Result.error(500, "Wiki知识库构建失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取知识库状态
     */
    @GetMapping("/admin/knowledge/status")
    public Result<Map<String, Object>> getKnowledgeStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        if (principal == null || principal.getRole() != UserRole.ADMIN) {
            return Result.error(403, "需要管理员权限");
        }
        
        // TODO: 实现知识库状态统计
        return Result.success(Map.of(
                "status", "正常",
                "message", "知识库状态查询功能待实现"
        ));
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

