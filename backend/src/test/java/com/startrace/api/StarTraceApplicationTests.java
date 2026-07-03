package com.startrace.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用上下文加载测试
 * 使用 Testcontainers 自动启动 PostgreSQL 容器
 */
@SpringBootTest
@ActiveProfiles("test")
class StarTraceApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常启动
    }
}
