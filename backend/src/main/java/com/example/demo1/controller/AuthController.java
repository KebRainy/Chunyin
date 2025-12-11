package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.LoginRequest;
import com.example.demo1.dto.request.RegisterRequest;
import com.example.demo1.dto.response.LoginResponse;
import com.example.demo1.dto.response.UserProfileVO;
import com.example.demo1.entity.User;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.JwtService;
import com.example.demo1.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse response) {
        User user = userService.register(request);
        setAuthCookie(httpRequest, response, jwtService.generateToken(user));
        UserProfileVO profile = userService.buildProfile(user, user.getId());
        return Result.success(new LoginResponse(profile));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse response) {
        User user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        setAuthCookie(httpRequest, response, jwtService.generateToken(user));
        UserProfileVO profile = userService.buildProfile(user, user.getId());
        return Result.success(new LoginResponse(profile));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        clearCookie(httpRequest, response);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserProfileVO> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userService.getRequiredUser(principal.getId());
        return Result.success(userService.buildProfile(user, principal.getId()));
    }

    private void setAuthCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .path("/api")
            .sameSite("Lax")
            .secure(shouldUseSecureCookie(request))
            .maxAge(Duration.ofMillis(expirationMillis))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .path("/api")
            .sameSite("Lax")
            .secure(shouldUseSecureCookie(request))
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean shouldUseSecureCookie(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && proto.equalsIgnoreCase("https");
    }
}

