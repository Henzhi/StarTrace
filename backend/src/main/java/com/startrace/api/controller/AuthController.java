package com.startrace.api.controller;

import com.startrace.api.common.ApiResponse;
import com.startrace.api.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 注册 / 登录 / Token 刷新
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getCaptchaToken()
        );
        return ApiResponse.ok("注册成功", new TokenResponse(token));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ApiResponse.ok("登录成功", new TokenResponse(token));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh() {
        // Sa-Token 自动处理 Token 续签
        // 请求到达时拦截器已校验 Token 有效性
        String token = cn.dev33.satoken.stp.StpUtil.getTokenValue();
        return ApiResponse.ok(new TokenResponse(token));
    }

    // ===== DTO =====

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度需在 6-100 之间")
        private String password;

        // v1: reCAPTCHA optional; 在拦截器中校验
        private String captchaToken;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    @RequiredArgsConstructor
    public static class TokenResponse {
        private final String token;
        private final String tokenType = "Bearer";
    }
}
