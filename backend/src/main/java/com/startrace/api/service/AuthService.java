package com.startrace.api.service;

import com.startrace.api.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param username 用户名，3-20 字符
     * @param password 明文密码
     * @param captchaToken reCAPTCHA v3 token
     * @return 注册成功后的 JWT Token
     */
    String register(String username, String password, String captchaToken);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录成功后的 JWT Token
     */
    String login(String username, String password);

    /**
     * 根据用户 ID 查询用户
     */
    User findById(Long id);
}
