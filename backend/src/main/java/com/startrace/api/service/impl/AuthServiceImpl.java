package com.startrace.api.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.startrace.api.common.BusinessException;
import com.startrace.api.entity.User;
import com.startrace.api.mapper.UserMapper;
import com.startrace.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public String register(String username, String password, String captchaToken) {
        // 1. 校验用户名唯一
        if (userMapper.existsByUsername(username)) {
            throw BusinessException.usernameTaken();
        }

        // 2. 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userMapper.insert(user);

        // 3. 执行登录，返回 Token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        log.info("User registered: id={}, username={}", user.getId(), username);
        return token;
    }

    @Override
    public String login(String username, String password) {
        // 1. 查找用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw BusinessException.invalidCredentials();
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BusinessException.invalidCredentials();
        }

        // 3. 执行登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        log.info("User logged in: id={}, username={}", user.getId(), username);
        return token;
    }

    @Override
    public User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户", id.toString());
        }
        return user;
    }
}
