package com.startrace.api.common;

import lombok.Getter;

/**
 * 业务异常，用于在 Service 层抛出可预见的错误
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(4001, message);
    }

    public static BusinessException notFound(String resource, String id) {
        return new BusinessException(ApiResponse.NOT_FOUND, resource + " 不存在: " + id);
    }

    public static BusinessException invalidCredentials() {
        return new BusinessException(ApiResponse.INVALID_CREDENTIALS, "用户名或密码错误");
    }

    public static BusinessException usernameTaken() {
        return new BusinessException(ApiResponse.USERNAME_TAKEN, "该用户名已被使用");
    }
}
