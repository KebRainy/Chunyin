package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.SharePostCommentRequest;
import com.example.demo1.dto.request.SharePostRequest;
import com.example.demo1.dto.response.SharePostCommentVO;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.SharePostService;
import com.example.demo1.service.UserService;
import com.example.demo1.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/circle")
@RequiredArgsConstructor
public class CircleController {

    private final SharePostService sharePostService;
    private final UserService userService;

    @PostMapping("/posts")
    public Result<SharePostVO> publish(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SharePostRequest request,
            HttpServletRequest servletRequest) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        SharePostVO vo = sharePostService.createPost(principal.getId(), request, IpUtils.getClientIp(servletRequest));
        return Result.success(vo);
    }

    @GetMapping("/posts")
    public Result<PageResult<SharePostVO>> list(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(sharePostService.listPosts(page, pageSize, currentUserId));
    }

    @GetMapping("/feed")
    public Result<PageResult<SharePostVO>> feed(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(sharePostService.listFollowingPosts(principal.getId(), page, pageSize));
    }

    @GetMapping("/nearby")
    public Result<PageResult<SharePostVO>> nearby(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String city,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(sharePostService.listPostsByCity(city, page, pageSize, currentUserId));
    }

    @GetMapping("/users/{userId}/posts")
    public Result<PageResult<SharePostVO>> userPosts(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(sharePostService.listPostsByUser(userId, page, pageSize, currentUserId));
    }

    @GetMapping("/posts/{id}")
    public Result<SharePostVO> getPost(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(sharePostService.getPostById(id, currentUserId, true));
    }

    @PostMapping("/posts/{id}/like")
    public Result<Boolean> like(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(sharePostService.toggleLike(id, principal.getId()));
    }

    @PostMapping("/posts/{id}/favorite")
    public Result<Boolean> favorite(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(sharePostService.toggleFavorite(id, principal.getId()));
    }

    @GetMapping("/posts/{id}/comments")
    public Result<List<SharePostCommentVO>> listComments(@PathVariable Long id) {
        return Result.success(sharePostService.listComments(id));
    }

    @PostMapping("/posts/{id}/comments")
    public Result<SharePostCommentVO> createComment(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SharePostCommentRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(sharePostService.createComment(id, principal.getId(), request));
    }
}
