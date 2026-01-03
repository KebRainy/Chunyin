package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.ChangePasswordRequest;
import com.example.demo1.dto.request.UpdateMessagePolicyRequest;
import com.example.demo1.dto.request.UpdateProfileRequest;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.dto.response.UserProfileVO;
import com.example.demo1.entity.User;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.UserService;
import com.example.demo1.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserProfileVO> profile(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal,
                                         HttpServletRequest request) {
        User user = userService.getRequiredUser(id);
        Long currentUserId = principal != null ? principal.getId() : null;
        String ipAddress = IpUtils.getClientIp(request);
        return Result.success(userService.buildProfile(user, currentUserId, ipAddress));
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody UpdateProfileRequest request,
                                               HttpServletRequest httpRequest) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updateProfile(principal.getId(), request);
        User user = userService.getRequiredUser(principal.getId());
        String ipAddress = IpUtils.getClientIp(httpRequest);
        return Result.success(userService.buildProfile(user, principal.getId(), ipAddress));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.changePassword(principal.getId(), request);
        return Result.success();
    }

    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id,
                               @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.follow(principal.getId(), id);
        return Result.success();
    }

    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.unfollow(principal.getId(), id);
        return Result.success();
    }

    @GetMapping("/followees")
    public Result<List<SimpleUserVO>> getFollowees(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<User> followees = userService.getFollowees(principal.getId());
        List<SimpleUserVO> result = followees.stream()
                .map(userService::buildSimpleUser)
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @PutMapping("/message-policy")
    public Result<Void> updateMessagePolicy(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody UpdateMessagePolicyRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updateMessagePolicy(principal.getId(), request);
        return Result.success();
    }

    /**
     * 取消推荐用户（不再推荐该用户）
     */
    @PostMapping("/{id}/block-recommend")
    public Result<Void> blockRecommendedUser(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.blockRecommendedUser(principal.getId(), id);
        return Result.success();
    }
}

