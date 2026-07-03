---
name: startrace-tech-stack
description: >
  星迹 (StarTrace) 项目完整技术栈选型参考。涵盖 Android 客户端（Kotlin + Compose + Room）和
  后端服务（Spring Boot 3 + Sa-Token + PostgreSQL + Redis）的全栈技术选型、版本约束、选型理由
  以及候选方案对比。当讨论或修改星迹项目的技术选型、搭建项目脚手架、评估新依赖引入时触发此 Skill。
agent_created: true
---

# 星迹 (StarTrace) 技术栈选型

## 概述

星迹是一个"离线优先"的 Android 灵感管理与 AI 故事创作应用，后端提供轻量用户系统和故事广场服务。技术选型遵循**少依赖、可验证、渐进式**原则——每个引入的库必须有不依赖它就无法高效完成的核心功能，v1 阶段所有创意功能均不依赖后端。

---

## 一、客户端技术栈 (Android)

### 1.1 核心基础

| 技术 | 版本 | 选型理由 | 备选方案 & 淘汰原因 |
|------|------|---------|-------------------|
| **Kotlin** | 2.0+ | Android 官方首选语言；协程原生支持异步/Flow；与 Compose Compiler 插件深度集成 | Java — 缺少协程、Flow，代码冗长 |
| **JDK** | 17 | Compose Compiler 和 Room KSP 均要求 JDK 17+ | JDK 11 — Compose 新版本已不支持 |
| **Min SDK** | API 26 (Android 8.0) | 覆盖 95%+ 活跃设备 | API 24 — 1% 额外覆盖率不值得放弃新 API |
| **Target SDK** | API 34 (Android 14) | 符合 Google Play 最新上架要求 | — |
| **Compile SDK** | API 35 | 使用最新编译时 API，同时保持向后兼容 | — |

### 1.2 UI 与交互

| 技术 | 版本 | 选型理由 | 备选方案 & 淘汰原因 |
|------|------|---------|-------------------|
| **Jetpack Compose** | 1.7+ (BOM) | Google 主推的声明式 UI；与 Kotlin 协程/Flow 原生配合；Canvas API 可直接自绘星系视图 | XML + ViewBinding — 无法做星系自绘；手勢处理复杂 |
| **Compose Canvas** | 内置 | 力导向图渲染、贝塞尔曲线连线、深空粒子背景，全部自绘实现 | D3.js (WebView) — 性能差、与原生手势割裂 |
| **Compose Navigation** | 2.8+ | 类型安全的声明式路由；深度链接原生支持；底部 Tab 导航内置 | 第三方导航库 — 增加依赖，Compose Navigation 已足够 |
| **Coil** (Compose) | 2.7+ | Kotlin 协程原生；轻量（无 Glide 的 Annotation Processor 开销）；Compose 集成最佳 | Glide — 基于 Java/APT，Compose 集成需要额外桥接 |
| **Material 3** | Compose BOM 内含 | 深空主题基础组件库；动态取色支持 | Material 2 — 已停止演进 |

### 1.3 架构与依赖注入

| 技术 | 版本 | 选型理由 | 备选方案 & 淘汰原因 |
|------|------|---------|-------------------|
| **Hilt** (Dagger) | 2.51+ | Google 推荐；编译时依赖图验证；与 ViewModel/Compose 深度集成 | Koin — 运行时 DI，缺少编译时验证；大项目中 Runtime 异常难以排查 |
| **ViewModel** | Compose BOM 内含 | 生命周期感知；Config Change 数据保留；Hilt 原生注入支持 | 手动生命周期管理 — 容易内存泄漏 |
| **StateFlow + UDF** | Kotlin 内置 | Compose 原生 collectAsStateWithLifecycle() 支持；类型安全的 sealed interface 状态建模 | LiveData — 不支持 sealed class；非 Kotlin 原生 |

### 1.4 数据持久化

| 技术 | 版本 | 选型理由 | 备选方案 & 淘汰原因 |
|------|------|---------|-------------------|
| **Room** | 2.6+ | 离线优先架构核心；Flow 响应式查询；编译时 SQL 校验；Migration 框架完善 | SQLDelight — Kotlin Multiplatform 更好，但 Android 单端无优势 |
| **DataStore** | 1.1+ | 替代 SharedPreferences；协程原生；支持 Flow 观察；Token/用户偏好存储 | SharedPreferences — 主线程阻塞；无类型安全；已不推荐 |
| **Android Keystore** | 系统 API | 硬件级 AES/GCM 加密存储用户 API Key；不依赖额外库 | EncryptedSharedPreferences — 功能重叠，Keystore 更底层可控 |

### 1.5 网络层

| 技术 | 版本 | 选型理由 | 备选方案 & 淘汰原因 |
|------|------|---------|-------------------|
| **OkHttp** | 4.12+ | Android 网络栈事实标准；拦截器链架构；支持 HTTP/2、连接池 | HttpURLConnection — 功能缺失 |
| **Retrofit** | 2.11+ | 声明式 API 接口；与 OkHttp/Moshi 无缝集成；注解驱动的请求构建 | Ktor Client — KMP 场景更好，Android 单端 Retrofit 生态更成熟 |
| **Moshi** | 1.15+ | Kotlin 原生 JSON 库；编译时代码生成；比 Gson 更 Kotlin 友好 | Gson — 基于 Java 反射，对 Kotlin null-safety/data class 支持不佳 |
| **自定义 SSE 解析器** | 自研 | LLM 流式输出解析；不依赖第三方 SSE 库（减少依赖）；精确控制 token 事件分发 | OkHttp SSE 插件 — 过度封装，自定义解析器更可控 |

### 1.6 测试工具链

| 技术 | 用途 | 选型理由 |
|------|------|---------|
| **JUnit 5** | 单元测试框架 | 参数化测试、DisplayName、扩展模型全面超越 JUnit 4 |
| **MockK** | Kotlin Mock 框架 | 原生支持 Kotlin 协程、扩展函数、object 的 mock；比 Mockito Kotlin 更自然 |
| **Turbine** | Flow 测试 | 简化 StateFlow/SharedFlow 的断言；支持 take/expect 模式 |
| **Room In-Memory** | DAO 集成测试 | 内存数据库快速启动；每个测试独立实例 |
| **Compose UI Test** | UI 自动化测试 | 官方支持；与 Compose 语义树配合；覆盖关键流程 |
| **Paparazzi** | 组件快照测试 | 无模拟器依赖的渲染截图对比；覆盖所有 UI 状态变体 |

### 1.7 可观测性

| 技术 | 用途 | 版本 |
|------|------|------|
| **Firebase Crashlytics** | 崩溃监控 | BoM 33+ |
| **Firebase Performance** | 启动/网络性能监控 | BoM 33+ |
| **Timber** | 结构化日志（Debug 全量 / Release 仅 WARN+） | 5.0+ |

---

## 二、后端技术栈 (Spring Boot)

### 2.1 核心框架

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| **Spring Boot** | 3.3+ | Java 生态最成熟的企业级框架；自动配置减少样板代码；生态丰富（监控/安全/数据） |
| **JDK** | 21 (LTS) | 当前最新 LTS，原生支持 Virtual Threads；Spring Boot 3.2+ 一等公民；LTS 支持至 2031 年，比 JDK 17 多 2 年 |
| **Gradle** (Kotlin DSL) | 8.x | 与 Android 端共用 Gradle 生态；Kotlin DSL 类型安全 |

### 2.2 安全与认证 (Sa-Token)

| 技术 | 版本 | 选型理由 | 备选方案 & 淘汰原因 |
|------|------|---------|-------------------|
| **Sa-Token** | 1.38+ | 轻量级 Java 权限认证框架；原生支持 JWT/Token/Redis 多种模式；API 比 Spring Security 简洁 10 倍；内置踢人下线、Token 续签、同端互斥等实用功能 | Spring Security — 配置极其复杂，学习曲线陡峭，单人开发成本高 |
| **Sa-Token Redis 集成** | sa-token-dao-redis-jackson | Token 持久化到 Redis；支持分布式会话 | — |
| **Google reCAPTCHA** | v3 | 注册环节防机器人；无感知验证（v3 不需要用户点击） | 腾讯验证码 — 国内体验更好但海外不可用；v1 选择覆盖面更广的方案 |

**Sa-Token 集成示例**：

```java
// 依赖
dependencies {
    implementation "cn.dev33:sa-token-spring-boot3-starter:1.38.0"
    implementation "cn.dev33:sa-token-dao-redis-jackson:1.38.0"
    implementation "cn.dev33:sa-token-jwt:1.38.0"  // 可选：集成 JWT
}

// application.yml
sa-token:
  token-name: Authorization       # Token 名称（前端 Header 中传递）
  timeout: 7200                   # Access Token 有效期（秒），2小时
  active-timeout: 1800            # 临时 Token 有效期（如果30分钟无操作则过期）
  is-concurrent: true             # 允许同账号并发登录
  is-share: true                  # 共用一个 Token
  token-style: random-64          # Token 风格
  is-log: false                   # 关闭全局日志

// 登录接口
@PostMapping("/api/v1/auth/login")
public SaResult login(@RequestBody LoginRequest request) {
    // 验证用户名密码
    User user = userService.login(request.getUsername(), request.getPassword());
    // 执行登录，返回 Token
    StpUtil.login(user.getId());
    String token = StpUtil.getTokenValue();
    return SaResult.ok("登录成功").set("token", token);
}

// 权限校验（注解式）
@GetMapping("/api/v1/stories/drafts")
@SaCheckLogin  // 仅登录即可访问
public SaResult getDrafts() { ... }

// 获取当前用户 ID
String userId = StpUtil.getLoginIdAsString();
```

**JDK 21 Virtual Threads 启用**（一行配置）：

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true   # Spring Boot 3.2+ 内置支持
```

> 传统平台线程池（Tomcat 默认 200 线程）在高并发时会因线程耗尽导致请求排队。Virtual Threads 轻量到可同时存在数万个，不会阻塞 OS 线程。故事广场在流量高峰时响应延迟更稳定。

### 2.3 数据层

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| **PostgreSQL** | 16+ | 成熟的关系型数据库；JSONB 支持半结构化数据；FTS 支持全文搜索 |
| **MyBatis-Plus** | 3.5+ | 比 JPA/Hibernate 更灵活的 SQL 控制；代码生成器减少 CRUD 样板；分页插件内置 |
| **Flyway** | 10+ | 数据库版本迁移；SQL 脚本管理 schema 变更；比 Liquibase 更轻量 |

### 2.4 缓存与中间件

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| **Redis** | 7+ | 验证码缓存（reCAPTCHA Token）；Token 持久化（Sa-Token Redis DAO）；未来热度排行 |
| **Spring Cache** + Redis | Spring Boot 内置 | 声明式缓存注解（@Cacheable/@CacheEvict）；与 Redis 透明集成 |

### 2.5 API 文档与测试

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| **SpringDoc OpenAPI** | 2.6+ | Swagger UI 自动生成；Spring Boot 3 原生支持；可导出 OpenAPI 3.0 规范 |
| **JUnit 5** | Spring Boot 内置 | 与 Spring Test 深度集成；@SpringBootTest 支持 |
| **Testcontainers** | 1.20+ | 集成测试用真实 PostgreSQL/Redis 容器；比 H2 内存库更接近生产环境 |

### 2.6 部署 — Docker 全容器化

#### 选型对比

| 方案 | 选型理由 | 备选方案 & 淘汰原因 |
|------|---------|-------------------|
| **Docker + Compose** | 所有服务（App / PostgreSQL / Redis / Nginx）容器化，一键 `docker compose up -d` 即可启动全部；环境一致性保证"我机器上能跑"不再是问题 | 裸机部署 — 环境不一致，运维成本高 |
| **Nginx 反向代理** | 终端 SSL 终结 + 静态资源缓存 + 请求速率限制，不侵入 Spring Boot 应用代码 | Caddy — 配置更简洁但生态不如 Nginx 成熟 |
| **GitHub Actions** | 代码推送自动触发镜像构建与推送；与 GitHub Container Registry 无缝集成 | Jenkins — 需要自建服务器，v1 阶段无必要 |
| **Docker Compose Watch** | 开发阶段热重载（改代码自动重建容器），无需手动重启 | — |

#### 目录结构

```
backend/
├── Dockerfile                    # Spring Boot 多阶段构建
├── docker-compose.yml            # 全服务编排
├── docker-compose.dev.yml        # 开发环境覆盖（热重载）
├── nginx/
│   ├── nginx.conf                # Nginx 主配置
│   └── conf.d/
│       └── startrace.conf        # 星迹站点配置
├── .env                          # Compose 环境变量（不提交 Git）
├── .env.example                  # 环境变量模板（提交 Git）
└── .dockerignore                 # 排除构建上下文中的无关文件
```

#### Dockerfile（Spring Boot 多阶段构建 + JDK 21）

```dockerfile
# ==================== 构建阶段 ====================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 先复制依赖描述文件，利用 Docker 缓存层
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY src/ src/

# 构建 fat jar
RUN ./gradlew bootJar --no-daemon -x test

# ==================== 运行阶段 ====================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser

# 复制 jar
COPY --from=builder /app/build/libs/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

USER appuser
EXPOSE 8080

# 启用 Virtual Threads（JDK 21 特性）
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### docker-compose.yml（生产编排）

```yaml
version: "3.9"

services:
  # ========== 数据库 ==========
  postgres:
    image: postgres:16-alpine
    container_name: startrace-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-startrace}
      POSTGRES_USER: ${POSTGRES_USER:-startrace}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./sql/init:/docker-entrypoint-initdb.d  # 初始化脚本（可选）
    ports:
      - "127.0.0.1:5432:5432"        # 仅本地可访问
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-startrace} -d ${POSTGRES_DB:-startrace}"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ========== 缓存 ==========
  redis:
    image: redis:7-alpine
    container_name: startrace-redis
    restart: unless-stopped
    command: >
      redis-server
      --appendonly yes
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    ports:
      - "127.0.0.1:6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ========== Spring Boot 应用 ==========
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: startrace-api:${APP_VERSION:-latest}
    container_name: startrace-api
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-startrace}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-startrace}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      SA_TOKEN_JWT_SECRET_KEY: ${JWT_SECRET}
      RECAPTCHA_SECRET_KEY: ${RECAPTCHA_SECRET}
    ports:
      - "127.0.0.1:8080:8080"
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 3

  # ========== 反向代理 ==========
  nginx:
    image: nginx:1.27-alpine
    container_name: startrace-nginx
    restart: unless-stopped
    depends_on:
      - app
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro       # SSL 证书目录
    ports:
      - "80:80"
      - "443:443"
    healthcheck:
      test: ["CMD", "nginx", "-t"]
      interval: 30s
      timeout: 5s
      retries: 3

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
```

#### docker-compose.dev.yml（开发覆盖）

```yaml
# 开发环境覆盖文件，使用方式：
#   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev    # 用开发版 Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: dev
    volumes:
      - ./build/libs:/app:ro        # 开发版：挂载 jar 以支持热重载
    develop:
      watch:                         # Docker Compose Watch（v2.22+）
        - action: rebuild
          path: ./build.gradle.kts
        - action: rebuild
          path: ./src

  postgres:
    ports:
      - "5432:5432"                  # 开发时允许外部连接数据库

  redis:
    ports:
      - "6379:6379"                  # 开发时允许外部连接 Redis
```

#### Nginx 配置 (startrace.conf)

```nginx
# 上游应用服务器
upstream startrace_api {
    server app:8080;
    keepalive 32;
}

# HTTP → HTTPS 强制跳转
server {
    listen 80;
    server_name api.startrace.app;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.startrace.app;

    # SSL 证书
    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 安全头
    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;

    # 速率限制
    limit_req_zone $binary_remote_addr zone=api:10m rate=30r/m;
    limit_req zone=api burst=10 nodelay;

    # 日志
    access_log /var/log/nginx/startrace_access.log;
    error_log  /var/log/nginx/startrace_error.log;

    location / {
        proxy_pass http://startrace_api;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
    }

    # 静态资源缓存
    location /swagger-ui/ {
        proxy_pass http://startrace_api;
        expires 7d;
    }
}
```

#### .env.example

```bash
# ===== 数据库 =====
POSTGRES_DB=startrace
POSTGRES_USER=startrace
POSTGRES_PASSWORD=changeme_use_strong_password

# ===== Redis =====
REDIS_PASSWORD=changeme_use_strong_password

# ===== JWT =====
JWT_SECRET=changeme_generate_via_openssl_rand_base64_64

# ===== Google reCAPTCHA =====
RECAPTCHA_SECRET=your_recaptcha_secret_key

# ===== Docker =====
APP_VERSION=latest
```

#### 镜像构建与推送 (GitHub Actions)

```yaml
# .github/workflows/docker-build.yml
name: Build & Push Docker Image

on:
  push:
    branches: [main]
    tags: ['v*']
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/startrace-api

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew test

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata (tags, labels)
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=sha,format=short

      - name: Build and push Docker image
        uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

#### 部署命令速查

```bash
# 首次部署
cp .env.example .env
#   → 编辑 .env，填入真实密码和密钥
docker compose up -d
docker compose ps                    # 确认所有服务健康

# 日常运维
docker compose down                  # 停止所有服务（保留数据卷）
docker compose restart app           # 仅重启应用
docker compose logs -f app           # 查看应用日志
docker compose exec postgres psql -U startrace  # 进入数据库

# 更新应用
git pull
docker compose build app             # 重建应用镜像
docker compose up -d app             # 滚动更新（仅重启 app）
docker system prune -a               # 清理旧镜像

# 数据备份
docker compose exec postgres pg_dump -U startrace startrace > backup_$(date +%Y%m%d).sql

# 紧急回滚
docker compose up -d app             # 用上一个版本的镜像重启
```

---

## 三、关键依赖版本锁定策略

### 原则

- **Android BOM (Bill of Materials)**：Compose / CameraX / Lifecycle 等 Jetpack 库统一由 BOM 管理版本，避免手动对齐
- **Spring Boot Parent POM**：由 `spring-boot-starter-parent` 统一管理所有 Spring 生态版本
- **Renovate/Dependabot**：自动 PR 检测依赖更新，但关键库（Room / Hilt / Compose Compiler）升级需人工审查

### Android Gradle 依赖管理示例

```kotlin
// build.gradle.kts (app module)
dependencies {
    // Compose BOM 统一版本
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
}
```

### Spring Boot Gradle 依赖管理示例

```kotlin
// build.gradle.kts (backend)
plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Sa-Token
    implementation("cn.dev33:sa-token-spring-boot3-starter:1.38.0")
    implementation("cn.dev33:sa-token-dao-redis-jackson:1.38.0")

    // Database
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // API Docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
}
```

---

## 四、快速启动命令

### Android 客户端

```bash
# 代码风格检查
./gradlew ktlintCheck detekt lintDebug

# 运行所有单元测试
./gradlew testDebugUnitTest

# 构建 Debug APK
./gradlew assembleDebug

# Room Schema 导出检查
# 检查 app/schemas/ 目录下 JSON 文件变更
```

### 后端服务

```bash
# 本地开发（需要 PostgreSQL + Redis）
./gradlew bootRun

# 使用 Docker Compose 一键启动所有服务
docker compose up -d

# 运行测试（含 Testcontainers）
./gradlew test

# API 文档
# 启动后访问 http://localhost:8080/swagger-ui.html
```

---

## 五、扩展阅读

该 Skill 聚焦技术选型本身。完整架构设计参见项目文档：

- `星迹_产品设计方案.md` — 功能规格、UI/UX、数据模型、开发路线图
- `星迹_系统架构设计.md` — ADR（架构决策记录）、组件架构、离线性策略、安全架构、测试策略、CI/CD、长期演进
