package com.startrace.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * 统一 API 响应格式
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... },
 *   "requestId": "uuid",
 *   "timestamp": "2026-07-03T02:30:00Z"
 * }
 * </pre>
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String requestId;
    private Instant timestamp;

    // ===== 成功响应 =====

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data, UUID.randomUUID().toString(), Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data, UUID.randomUUID().toString(), Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "success", null, UUID.randomUUID().toString(), Instant.now());
    }

    // ===== 错误响应 =====

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, UUID.randomUUID().toString(), Instant.now());
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, UUID.randomUUID().toString(), Instant.now());
    }

    // ===== 常用状态码 =====

    public static final int UNAUTHORIZED = 1001;
    public static final int TOKEN_EXPIRED = 1002;
    public static final int INVALID_CREDENTIALS = 1003;
    public static final int USERNAME_TAKEN = 1004;
    public static final int FORBIDDEN = 2001;
    public static final int NOT_FOUND = 3001;
    public static final int VALIDATION_ERROR = 4001;
    public static final int CAPTCHA_FAILED = 4002;
    public static final int INTERNAL_ERROR = 5001;
}
