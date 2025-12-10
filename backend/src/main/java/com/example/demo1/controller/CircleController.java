package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.SharePostRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<PageResult<SharePostVO>> list(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(sharePostService.listPosts(page, pageSize));
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
}
