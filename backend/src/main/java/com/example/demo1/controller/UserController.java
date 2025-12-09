package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.UpdateProfileRequest;
import com.example.demo1.dto.response.UserProfileVO;
import com.example.demo1.entity.User;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.UserService;
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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserProfileVO> profile(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getRequiredUser(id);
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(userService.buildProfile(user, currentUserId));
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updateProfile(principal.getId(), request);
        User user = userService.getRequiredUser(principal.getId());
        return Result.success(userService.buildProfile(user, principal.getId()));
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
}


