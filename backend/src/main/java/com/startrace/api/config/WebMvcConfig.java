package com.startrace.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — Sa-Token 路由拦截
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        // 公开接口
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/stories",              // GET 公开浏览
                        "/api/v1/stories/*",             // GET 公开浏览
                        // Swagger
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        // Actuator
                        "/actuator/**"
                );
    }
}
